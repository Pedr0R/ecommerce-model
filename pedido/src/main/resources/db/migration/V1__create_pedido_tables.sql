CREATE TABLE IF NOT EXISTS tb_pedidos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    valor_total NUMERIC(38, 2),
    status VARCHAR(255),
    data_criacao TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_itens_pedido (
    id BIGSERIAL PRIMARY KEY,
    produto_id VARCHAR(255),
    quantidade INTEGER,
    preco_unitario NUMERIC(38, 2),
    pedido_id BIGINT,
    CONSTRAINT fk_itens_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES tb_pedidos(id) ON DELETE CASCADE
);
