package p.ramos.ms.pedido.dto;

import java.math.BigDecimal;

public record PedidoCriadoEvent(
    Long pedidoId,
    Long usuarioId,
    BigDecimal valorTotal
) {}
