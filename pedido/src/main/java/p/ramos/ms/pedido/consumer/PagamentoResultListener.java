package p.ramos.ms.pedido.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import p.ramos.ms.pedido.dto.PagamentoStatusEvent;
import p.ramos.ms.pedido.model.StatusPedido;
import p.ramos.ms.pedido.service.PedidoService;

@Component
public class PagamentoResultListener {

    private final PedidoService pedidoService;

    public PagamentoResultListener(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @RabbitListener(queues = "fila_pedido_status")
    public void consumirResultadoPagamento(PagamentoStatusEvent event) {
        System.out.println(">>> [PEDIDO] Resultado de pagamento recebido - Pedido ID: " + event.pedidoId() + " | Status: " + event.status());

        StatusPedido novoStatus = "APROVADO".equalsIgnoreCase(event.status()) 
                ? StatusPedido.PAGO 
                : StatusPedido.RECUSADO;

        try {
            pedidoService.updateStatus(event.pedidoId(), novoStatus);
            System.out.println(">>> [PEDIDO] Pedido ID: " + event.pedidoId() + " atualizado para " + novoStatus);
        } catch (Exception ex) {
            System.err.println(">>> [PEDIDO] Erro ao atualizar status do pedido " + event.pedidoId() + ": " + ex.getMessage());
            throw ex;
        }
    }
}
