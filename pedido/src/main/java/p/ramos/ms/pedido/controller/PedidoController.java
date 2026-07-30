package p.ramos.ms.pedido.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import p.ramos.ms.pedido.dto.PedidoRequestDTO;
import p.ramos.ms.pedido.dto.PedidoResponseDTO;
import p.ramos.ms.pedido.model.StatusPedido;
import p.ramos.ms.pedido.service.PedidoService;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> getAll() {
        return ResponseEntity.ok(pedidoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> checkout(@Valid @RequestBody PedidoRequestDTO request) {
        PedidoResponseDTO response = pedidoService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PedidoResponseDTO> updateStatus(@PathVariable Long id, @RequestParam StatusPedido status) {
        PedidoResponseDTO response = pedidoService.updateStatus(id, status);
        return ResponseEntity.ok(response);
    }
}
