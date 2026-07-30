package p.ramos.ms.pedido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PedidoRequestDTO(
    @NotNull(message = "O ID do usuário é obrigatório")
    Long usuarioId,

    @NotEmpty(message = "O pedido precisa conter pelo menos um item")
    @Valid
    List<ItemPedidoRequestDTO> itens
) {}
