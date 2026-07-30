package p.ramos.ms.pedido.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Main Exchange
    public static final String PEDIDO_EXCHANGE = "pedido-exchange";
    
    // DLQ Exchange
    public static final String PEDIDO_EXCHANGE_DLQ = "pedido-exchange-dlq";

    // Queues
    public static final String FILA_PAGAMENTOS = "fila_pagamentos";
    public static final String FILA_PAGAMENTOS_DLQ = "fila_pagamentos_dlq";
    
    public static final String FILA_PEDIDO_STATUS = "fila_pedido_status";
    public static final String FILA_PEDIDO_STATUS_DLQ = "fila_pedido_status_dlq";

    public static final String FILA_NOTIFICACOES = "fila_notificacoes";
    public static final String FILA_NOTIFICACOES_DLQ = "fila_notificacoes_dlq";

    // Routing Keys
    public static final String RK_PEDIDO_CRIADO = "pedido.criado";
    public static final String RK_PEDIDO_CRIADO_DLQ = "pedido.criado.dlq";
    
    public static final String RK_PAGAMENTO_STATUS = "pagamento.*";
    public static final String RK_PAGAMENTO_STATUS_DLQ = "pagamento.*.dlq";

    @Bean
    public TopicExchange pedidoExchange() {
        return new TopicExchange(PEDIDO_EXCHANGE);
    }

    @Bean
    public TopicExchange pedidoExchangeDlq() {
        return new TopicExchange(PEDIDO_EXCHANGE_DLQ);
    }

    // --- PAGAMENTOS CONFIG ---
    @Bean
    public Queue pagamentoQueue() {
        return QueueBuilder.durable(FILA_PAGAMENTOS)
                .withArgument("x-dead-letter-exchange", PEDIDO_EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", RK_PEDIDO_CRIADO_DLQ)
                .build();
    }

    @Bean
    public Queue pagamentoQueueDlq() {
        return QueueBuilder.durable(FILA_PAGAMENTOS_DLQ).build();
    }

    @Bean
    public Binding bindingPagamento() {
        return BindingBuilder.bind(pagamentoQueue()).to(pedidoExchange()).with(RK_PEDIDO_CRIADO);
    }

    @Bean
    public Binding bindingPagamentoDlq() {
        return BindingBuilder.bind(pagamentoQueueDlq()).to(pedidoExchangeDlq()).with(RK_PEDIDO_CRIADO_DLQ);
    }

    // --- PEDIDO STATUS CONFIG ---
    @Bean
    public Queue pedidoStatusQueue() {
        return QueueBuilder.durable(FILA_PEDIDO_STATUS)
                .withArgument("x-dead-letter-exchange", PEDIDO_EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", "pagamento.status.dlq")
                .build();
    }

    @Bean
    public Queue pedidoStatusQueueDlq() {
        return QueueBuilder.durable(FILA_PEDIDO_STATUS_DLQ).build();
    }

    @Bean
    public Binding bindingPedidoStatus() {
        return BindingBuilder.bind(pedidoStatusQueue()).to(pedidoExchange()).with(RK_PAGAMENTO_STATUS);
    }

    @Bean
    public Binding bindingPedidoStatusDlq() {
        return BindingBuilder.bind(pedidoStatusQueueDlq()).to(pedidoExchangeDlq()).with("pagamento.status.dlq");
    }

    // --- NOTIFICACOES CONFIG ---
    @Bean
    public Queue notificacoesQueue() {
        return QueueBuilder.durable(FILA_NOTIFICACOES)
                .withArgument("x-dead-letter-exchange", PEDIDO_EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", "notificacao.envio.dlq")
                .build();
    }

    @Bean
    public Queue notificacoesQueueDlq() {
        return QueueBuilder.durable(FILA_NOTIFICACOES_DLQ).build();
    }

    @Bean
    public Binding bindingNotificacoes() {
        return BindingBuilder.bind(notificacoesQueue()).to(pedidoExchange()).with(RK_PAGAMENTO_STATUS);
    }

    @Bean
    public Binding bindingNotificacoesDlq() {
        return BindingBuilder.bind(notificacoesQueueDlq()).to(pedidoExchangeDlq()).with("notificacao.envio.dlq");
    }

    // JSON Message Converter for serialization
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
