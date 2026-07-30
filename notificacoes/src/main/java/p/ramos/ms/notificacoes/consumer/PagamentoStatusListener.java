package p.ramos.ms.notificacoes.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import p.ramos.ms.notificacoes.dto.PagamentoStatusEvent;

@Component
public class PagamentoStatusListener {

    @RabbitListener(queues = "fila_notificacoes")
    public void consumirStatusPagamento(PagamentoStatusEvent event) {
        System.out.println(">>> [NOTIFICACOES] Recebido status de pagamento - Pedido ID: " + event.pedidoId() + " | Status: " + event.status());

        String mensagem = "APROVADO".equalsIgnoreCase(event.status())
                ? "Olá usuário " + event.usuarioId() + ", seu pagamento do pedido #" + event.pedidoId() + " no valor de R$ " + event.valor() + " foi aprovado com sucesso! Preparando envio."
                : "Olá usuário " + event.usuarioId() + ", lamentamos, mas o pagamento do seu pedido #" + event.pedidoId() + " no valor de R$ " + event.valor() + " foi recusado.";

        System.out.println("=================================================================================");
        System.out.println("📧 [SIMULAÇÃO DE E-MAIL ENVIADO]");
        System.out.println("Para: usuario_" + event.usuarioId() + "@email.com");
        System.out.println("Assunto: Status do Pedido #" + event.pedidoId());
        System.out.println("Mensagem: " + mensagem);
        System.out.println("=================================================================================");
    }
}
