package p.ramos.ms.pagamento.dto;

import java.math.BigDecimal;

public record PagamentoStatusEvent(
    Long pedidoId,
    Long usuarioId,
    BigDecimal valor,
    String status
) {}
