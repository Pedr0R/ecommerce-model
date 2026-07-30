package p.ramos.ms.pedido.dto;

import java.math.BigDecimal;

public record ProductDTO(
    String id,
    String nome,
    BigDecimal preco,
    Integer estoque
) {}
