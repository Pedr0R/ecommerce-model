package p.ramos.ms.notificacoes.dto;

import java.math.BigDecimal;

public record PagamentoStatusEvent(
    Long pedidoId,
    Long usuarioId,
    BigDecimal valor,
    String status
) {}
