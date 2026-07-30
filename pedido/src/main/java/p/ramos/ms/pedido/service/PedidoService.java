package p.ramos.ms.pedido.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import p.ramos.ms.pedido.config.RabbitMQConfig;
import p.ramos.ms.pedido.dto.ItemPedidoRequestDTO;
import p.ramos.ms.pedido.dto.ItemPedidoResponseDTO;
import p.ramos.ms.pedido.dto.PedidoRequestDTO;
import p.ramos.ms.pedido.dto.PedidoResponseDTO;
import p.ramos.ms.pedido.dto.ProductDTO;
import p.ramos.ms.pedido.dto.UserDTO;
import p.ramos.ms.pedido.dto.PedidoCriadoEvent;
import p.ramos.ms.pedido.dto.PagamentoStatusEvent;
import p.ramos.ms.pedido.model.ItemPedido;
import p.ramos.ms.pedido.model.Pedido;
import p.ramos.ms.pedido.model.StatusPedido;
import p.ramos.ms.pedido.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final String USERS_SERVICE_URL = "http://localhost:8085/users/";
    private static final String CATALOGO_SERVICE_URL = "http://localhost:8084/produtos/";

    public PedidoService(PedidoRepository pedidoRepository, RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        this.pedidoRepository = pedidoRepository;
        this.restTemplate = restTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> findAll() {
        return pedidoRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO findById(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com ID: " + id));
        return convertToResponseDTO(pedido);
    }

    @Transactional
    public PedidoResponseDTO checkout(PedidoRequestDTO request) {
        // 1. Validar Usuário no microsserviço de usuários
        try {
            restTemplate.getForObject(USERS_SERVICE_URL + request.usuarioId(), UserDTO.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RuntimeException("Usuário não encontrado com ID: " + request.usuarioId());
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao comunicar com o serviço de usuários: " + ex.getMessage());
        }

        // 2. Criar Pedido
        Pedido pedido = new Pedido(request.usuarioId(), BigDecimal.ZERO, StatusPedido.AGUARDANDO_PAGAMENTO, LocalDateTime.now());
        BigDecimal total = BigDecimal.ZERO;

        // 3. Processar Itens
        for (ItemPedidoRequestDTO itemDto : request.itens()) {
            ProductDTO produto;
            // 3.1 Consultar Produto no Catálogo
            try {
                produto = restTemplate.getForObject(CATALOGO_SERVICE_URL + itemDto.produtoId(), ProductDTO.class);
            } catch (HttpClientErrorException.NotFound ex) {
                throw new RuntimeException("Produto não encontrado com ID: " + itemDto.produtoId());
            } catch (Exception ex) {
                throw new RuntimeException("Erro ao comunicar com o serviço de catálogo: " + ex.getMessage());
            }

            if (produto == null) {
                throw new RuntimeException("Produto não encontrado com ID: " + itemDto.produtoId());
            }

            // 3.2 Verificar Estoque
            if (produto.estoque() < itemDto.quantidade()) {
                throw new RuntimeException("Estoque insuficiente para o produto: " + produto.nome() + " (Estoque: " + produto.estoque() + ")");
            }

            // 3.3 Decrementar Estoque no Catálogo (Integração REST)
            try {
                String decrementUrl = CATALOGO_SERVICE_URL + produto.id() + "/decrementar?quantidade=" + itemDto.quantidade();
                restTemplate.exchange(decrementUrl, HttpMethod.PUT, HttpEntity.EMPTY, Void.class);
            } catch (Exception ex) {
                throw new RuntimeException("Erro ao decrementar estoque para o produto " + produto.nome() + ": " + ex.getMessage());
            }

            // 3.4 Calcular Subtotal e Adicionar
            BigDecimal precoUnitario = produto.preco();
            BigDecimal subtotal = precoUnitario.multiply(BigDecimal.valueOf(itemDto.quantidade()));
            total = total.add(subtotal);

            ItemPedido item = new ItemPedido(produto.id(), itemDto.quantidade(), precoUnitario);
            pedido.adicionarItem(item);
        }

        pedido.setValorTotal(total);
        Pedido savedPedido = pedidoRepository.save(pedido);

        // Publicar evento "pedido.criado" no RabbitMQ
        PedidoCriadoEvent event = new PedidoCriadoEvent(
                savedPedido.getId(),
                savedPedido.getUsuarioId(),
                savedPedido.getValorTotal()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PEDIDO_EXCHANGE,
                RabbitMQConfig.RK_PEDIDO_CRIADO,
                event
        );

        return convertToResponseDTO(savedPedido);
    }

    @Transactional
    public PedidoResponseDTO updateStatus(Long id, StatusPedido status) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com ID: " + id));

        StatusPedido oldStatus = pedido.getStatus();

        // Se o pagamento for recusado, devolvemos o estoque no catálogo
        if (status == StatusPedido.RECUSADO && oldStatus == StatusPedido.AGUARDANDO_PAGAMENTO) {
            devolverEstoque(pedido);
        }

        pedido.setStatus(status);
        Pedido updated = pedidoRepository.save(pedido);

        // Publicar evento no RabbitMQ se o status mudou
        if (oldStatus != status) {
            PagamentoStatusEvent event = new PagamentoStatusEvent(
                    updated.getId(),
                    updated.getUsuarioId(),
                    updated.getValorTotal(),
                    status.name()
            );
            String routingKey = "pedido.status." + status.name().toLowerCase();
            rabbitTemplate.convertAndSend(RabbitMQConfig.PEDIDO_EXCHANGE, routingKey, event);
            System.out.println(">>> [PEDIDO] Evento de atualizacao de status publicado: " + routingKey);
        }

        return convertToResponseDTO(updated);
    }

    private void devolverEstoque(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            try {
                String incrementUrl = CATALOGO_SERVICE_URL + item.getProdutoId() + "/incrementar?quantidade=" + item.getQuantidade();
                restTemplate.exchange(incrementUrl, HttpMethod.PUT, HttpEntity.EMPTY, Void.class);
            } catch (Exception ex) {
                System.err.println("Erro ao devolver estoque para o produto " + item.getProdutoId() + ": " + ex.getMessage());
            }
        }
    }

    private PedidoResponseDTO convertToResponseDTO(Pedido pedido) {
        List<ItemPedidoResponseDTO> itensDto = pedido.getItens().stream()
                .map(item -> new ItemPedidoResponseDTO(item.getId(), item.getProdutoId(), item.getQuantidade(), item.getPrecoUnitario()))
                .collect(Collectors.toList());

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getUsuarioId(),
                pedido.getValorTotal(),
                pedido.getStatus(),
                pedido.getDataCriacao(),
                itensDto
        );
    }
}
