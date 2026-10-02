package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestor;

    private PedidoRequest pedido(Long clienteId, String email, ItemPedido... items) {
        return new PedidoRequest(clienteId, email, List.of(items));
    }

    @Test
    void stockInsuficienteSeRechaza() {
        // Producto 4 tiene stock 2; se piden 5 unidades.
        ResultadoPedido r = gestor.procesarPedido(
            pedido(1L, "vip@tienda.com", new ItemPedido(4L, 5)));
        assertFalse(r.isConfirmado());
        assertTrue(r.getMotivoRechazo().startsWith("Stock insuficiente"));
    }

    @Test
    void clienteInexistenteSeRechaza() {
        ResultadoPedido r = gestor.procesarPedido(
            pedido(999L, "nadie@tienda.com", new ItemPedido(1L, 1)));
        assertFalse(r.isConfirmado());
        assertEquals("Cliente no registrado", r.getMotivoRechazo());
    }

    @Test
    void clienteMorosoConDeudaDependeDelHorarioDeCorte() {
        ResultadoPedido r = gestor.procesarPedido(
            pedido(3L, "moroso@tienda.com", new ItemPedido(1L, 1)));
        boolean antesDeCorte = LocalTime.now().isBefore(LocalTime.of(20, 0));
        if (antesDeCorte) {
            // Dentro del horario de corte: se rechaza por deuda pendiente.
            assertFalse(r.isConfirmado());
            assertTrue(r.getMotivoRechazo().startsWith("Cliente con deuda pendiente"));
        } else {
            // Fuera del horario de corte: se permite excepcionalmente.
            assertTrue(r.isConfirmado());
        }
    }

    @Test
    void clienteMorosoSinDeudaSeConfirma() {
        // Cliente 4 es MOROSO pero no tiene facturas impagas.
        ResultadoPedido r = gestor.procesarPedido(
            pedido(4L, "aldia@tienda.com", new ItemPedido(2L, 1)));
        assertTrue(r.isConfirmado());
    }

    @Test
    void clienteVipRecibeDescuentoPorTramoDeSubtotal() {
        // Subtotal = 100000 * 8 = 800000 -> tramo > 500000 -> 10%.
        ResultadoPedido r = gestor.procesarPedido(
            pedido(1L, "vip@tienda.com", new ItemPedido(1L, 8)));
        assertTrue(r.isConfirmado());
        double esperado = calcularTotal(800000, 0.10);
        assertEquals(esperado, r.getTotal(), 0.001);
    }

    @Test
    void clienteFrecuenteConMasDeDiezPedidosRecibeOchoPorCiento() {
        // Subtotal = 100000 * 2 = 200000; cliente 2 tiene 11 pedidos previos -> 8%.
        ResultadoPedido r = gestor.procesarPedido(
            pedido(2L, "frecuente@tienda.com", new ItemPedido(1L, 2)));
        assertTrue(r.isConfirmado());
        double esperado = calcularTotal(200000, 0.08);
        assertEquals(esperado, r.getTotal(), 0.001);
    }

    private double calcularTotal(double subtotal, double descuento) {
        double base = subtotal - subtotal * descuento;
        return base + base * 0.19;
    }
}
