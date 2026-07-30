package p.ramos.ms.pedido.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequestDTO(
    @NotBlank(message = "O ID do produto é obrigatório")
    String produtoId,

    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade mínima de compra é 1")
    Integer quantidade
) {}
