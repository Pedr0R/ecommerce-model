package p.ramos.ms.pedido.dto;

import java.math.BigDecimal;

public record PagamentoStatusEvent(
    Long pedidoId,
    Long usuarioId,
    BigDecimal valor,
    String status
) {}
