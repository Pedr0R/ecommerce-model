package p.ramos.ms.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProdutoRequestDTO(
    @NotBlank(message = "O nome do produto é obrigatório")
    String nome,

    @NotBlank(message = "A descrição é obrigatória")
    String descricao,

    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
    BigDecimal preco,

    @NotBlank(message = "A categoria é obrigatória")
    String categoria,

    @NotNull(message = "A quantidade em estoque é obrigatória")
    @Min(value = 0, message = "O estoque não pode ser negativo")
    Integer estoque
) {}
