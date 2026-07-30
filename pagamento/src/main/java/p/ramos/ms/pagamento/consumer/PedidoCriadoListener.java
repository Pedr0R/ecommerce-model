package p.ramos.ms.pagamento.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import p.ramos.ms.pagamento.config.RabbitMQConfig;
import p.ramos.ms.pagamento.dto.PedidoCriadoEvent;
import p.ramos.ms.pagamento.dto.PagamentoStatusEvent;
import p.ramos.ms.pagamento.model.Pagamento;
import p.ramos.ms.pagamento.repository.PagamentoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class PedidoCriadoListener {

    private final PagamentoRepository pagamentoRepository;
    private final RabbitTemplate rabbitTemplate;

    public PedidoCriadoListener(PagamentoRepository pagamentoRepository, RabbitTemplate rabbitTemplate) {
        this.pagamentoRepository = pagamentoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "fila_pagamentos")
    public void consumirPedidoCriado(PedidoCriadoEvent event) {
        System.out.println(">>> [PAGAMENTO] Evento recebido - Pedido ID: " + event.pedidoId() + " | Valor: R$ " + event.valorTotal());

        // Regra de Mock: Recusar pagamentos acima de R$ 1000.00
        String status = "APROVADO";
        if (event.valorTotal().compareTo(BigDecimal.valueOf(1000.00)) > 0) {
            status = "RECUSADO";
        }

        // Salvar transação no banco local
        Pagamento pagamento = new Pagamento(
                event.pedidoId(),
                event.usuarioId(),
                event.valorTotal(),
                status,
                LocalDateTime.now()
        );
        pagamentoRepository.save(pagamento);
        System.out.println(">>> [PAGAMENTO] Transação de pagamento salva com status: " + status);

        // Publicar resultado de volta no RabbitMQ
        PagamentoStatusEvent responseEvent = new PagamentoStatusEvent(
                event.pedidoId(),
                event.usuarioId(),
                event.valorTotal(),
                status
        );

        String routingKey = "pagamento." + status.toLowerCase(); // pagamento.aprovado ou pagamento.recusado
        rabbitTemplate.convertAndSend(RabbitMQConfig.PEDIDO_EXCHANGE, routingKey, responseEvent);
        System.out.println(">>> [PAGAMENTO] Evento publicado no Exchange '" + RabbitMQConfig.PEDIDO_EXCHANGE + "' com RoutingKey '" + routingKey + "'");
    }
}
