package p.ramos.ms.pedido.dto;

import java.math.BigDecimal;

public record ItemPedidoResponseDTO(
    Long id,
    String produtoId,
    Integer quantidade,
    BigDecimal precoUnitario
) {}
