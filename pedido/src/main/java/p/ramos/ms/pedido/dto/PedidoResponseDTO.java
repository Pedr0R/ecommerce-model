package p.ramos.ms.pedido.dto;

import p.ramos.ms.pedido.model.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(
    Long id,
    Long usuarioId,
    BigDecimal valorTotal,
    StatusPedido status,
    LocalDateTime dataCriacao,
    List<ItemPedidoResponseDTO> itens
) {}
