package p.ramos.ms.catalogo.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import p.ramos.ms.catalogo.dto.ProdutoRequestDTO;
import p.ramos.ms.catalogo.dto.ProdutoResponseDTO;
import p.ramos.ms.catalogo.service.ProdutoService;

import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> getAll() {
        return ResponseEntity.ok(produtoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> getById(@PathVariable String id) {
        return ResponseEntity.ok(produtoService.findById(id));
    }

    @GetMapping("/busca")
    public ResponseEntity<List<ProdutoResponseDTO>> searchByName(@RequestParam String nome) {
        return ResponseEntity.ok(produtoService.searchByName(nome));
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<ProdutoResponseDTO>> getByCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(produtoService.findByCategoria(categoria));
    }

    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> create(@Valid @RequestBody ProdutoRequestDTO request) {
        ProdutoResponseDTO created = produtoService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> update(@PathVariable String id, @Valid @RequestBody ProdutoRequestDTO request) {
        ProdutoResponseDTO updated = produtoService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        produtoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/decrementar")
    public ResponseEntity<Void> decrementStock(@PathVariable String id, @RequestParam Integer quantidade) {
        produtoService.decrementarEstoque(id, quantidade);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/incrementar")
    public ResponseEntity<Void> incrementStock(@PathVariable String id, @RequestParam Integer quantidade) {
        produtoService.incrementarEstoque(id, quantidade);
        return ResponseEntity.noContent().build();
    }
}
