package p.ramos.ms.notificacoes.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import p.ramos.ms.notificacoes.dto.PagamentoStatusEvent;

@Component
public class PagamentoStatusListener {

    @RabbitListener(queues = "fila_notificacoes")
    public void consumirStatusPagamento(PagamentoStatusEvent event) {
        System.out.println(">>> [NOTIFICACOES] Recebido status de pedido - Pedido ID: " + event.pedidoId() + " | Status: " + event.status());

        String status = event.status().toUpperCase();
        String mensagem;

        switch (status) {
            case "APROVADO", "PAGO" -> 
                mensagem = "Olá usuário " + event.usuarioId() + ", seu pagamento do pedido #" + event.pedidoId() + " no valor de R$ " + event.valor() + " foi aprovado com sucesso! Preparando envio.";
            case "RECUSADO" -> 
                mensagem = "Olá usuário " + event.usuarioId() + ", lamentamos, mas o pagamento do seu pedido #" + event.pedidoId() + " no valor de R$ " + event.valor() + " foi recusado.";
            case "PREPARANDO" -> 
                mensagem = "Olá usuário " + event.usuarioId() + ", seu pedido #" + event.pedidoId() + " está em preparação. Estamos separando e embalando seus produtos!";
            case "EM_TRANSPORTE" -> 
                mensagem = "Olá usuário " + event.usuarioId() + ", ótimas notícias! Seu pedido #" + event.pedidoId() + " foi enviado e já está em transporte rumo ao seu endereço.";
            case "ENTREGUE" -> 
                mensagem = "Olá usuário " + event.usuarioId() + ", seu pedido #" + event.pedidoId() + " foi entregue com sucesso! Agradecemos a preferência.";
            default -> 
                mensagem = "Olá usuário " + event.usuarioId() + ", seu pedido #" + event.pedidoId() + " teve o status atualizado para: " + status;
        }

        System.out.println("=================================================================================");
        System.out.println("📧 [SIMULAÇÃO DE E-MAIL ENVIADO]");
        System.out.println("Para: usuario_" + event.usuarioId() + "@email.com");
        System.out.println("Assunto: Status do Pedido #" + event.pedidoId() + " - " + status);
        System.out.println("Mensagem: " + mensagem);
        System.out.println("=================================================================================");
    }
}
