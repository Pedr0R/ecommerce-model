package p.ramos.ms.catalogo.service;

import org.springframework.stereotype.Service;
import p.ramos.ms.catalogo.dto.ProdutoRequestDTO;
import p.ramos.ms.catalogo.dto.ProdutoResponseDTO;
import p.ramos.ms.catalogo.model.Produto;
import p.ramos.ms.catalogo.repository.ProdutoRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<ProdutoResponseDTO> findAll() {
        return produtoRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public ProdutoResponseDTO findById(String id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
        return convertToResponseDTO(produto);
    }

    public List<ProdutoResponseDTO> findByCategoria(String categoria) {
        return produtoRepository.findByCategoria(categoria).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ProdutoResponseDTO> searchByName(String nome) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome).stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public ProdutoResponseDTO save(ProdutoRequestDTO request) {
        Produto produto = new Produto(
                request.nome(),
                request.descricao(),
                request.preco(),
                request.categoria(),
                request.estoque()
        );
        Produto saved = produtoRepository.save(produto);
        return convertToResponseDTO(saved);
    }

    public ProdutoResponseDTO update(String id, ProdutoRequestDTO request) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));

        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());
        produto.setCategoria(request.categoria());
        produto.setEstoque(request.estoque());

        Produto updated = produtoRepository.save(produto);
        return convertToResponseDTO(updated);
    }

    public void deleteById(String id) {
        if (!produtoRepository.existsById(id)) {
            throw new RuntimeException("Produto não encontrado com ID: " + id);
        }
        produtoRepository.deleteById(id);
    }

    public void decrementarEstoque(String id, Integer quantidade) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));

        if (produto.getEstoque() < quantidade) {
            throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
        }

        produto.setEstoque(produto.getEstoque() - quantidade);
        produtoRepository.save(produto);
    }

    public void incrementarEstoque(String id, Integer quantidade) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));

        produto.setEstoque(produto.getEstoque() + quantidade);
        produtoRepository.save(produto);
    }

    private ProdutoResponseDTO convertToResponseDTO(Produto produto) {
        return new ProdutoResponseDTO(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getCategoria(),
                produto.getEstoque()
        );
    }
}
