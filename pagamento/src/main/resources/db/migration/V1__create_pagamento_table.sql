CREATE TABLE IF NOT EXISTS tb_pagamentos (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT,
    usuario_id BIGINT,
    valor NUMERIC(38, 2),
    status VARCHAR(255),
    data_processamento TIMESTAMP
);
