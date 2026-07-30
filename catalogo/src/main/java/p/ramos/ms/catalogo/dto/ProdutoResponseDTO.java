package p.ramos.ms.catalogo.dto;

import java.math.BigDecimal;

public record ProdutoResponseDTO(
    String id,
    String nome,
    String descricao,
    BigDecimal preco,
    String categoria,
    Integer estoque
) {}
