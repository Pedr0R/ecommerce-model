package p.ramos.ms.pagamento.dto;

import java.math.BigDecimal;

public record PedidoCriadoEvent(
    Long pedidoId,
    Long usuarioId,
    BigDecimal valorTotal
) {}
