package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Black Friday inactivo por defecto: se prueban las campanas Corporativo y Volumen.
@SpringBootTest
class CampanasDescuentoTest {

    @Autowired
    private GestorPedidos gestor;

    private PedidoRequest pedido(Long clienteId, String email, ItemPedido... items) {
        return new PedidoRequest(clienteId, email, List.of(items));
    }

    private double total(double subtotal, double descuento) {
        double base = subtotal - subtotal * descuento;
        return base + base * 0.19;
    }

    @Test
    void campanaCorporativoAplicaDiezPorCientoAClienteConNit() {
        // Cliente 5 es ESTANDAR con NIT; subtotal = 100000.
        ResultadoPedido r = gestor.procesarPedido(
            pedido(5L, "corp@tienda.com", new ItemPedido(1L, 1)));
        assertTrue(r.isConfirmado());
        assertEquals(total(100000, 0.10), r.getTotal(), 0.001);
    }

    @Test
    void campanaVolumenAplicaDocePorCientoAPedidoDeMasDeVeinteUnidades() {
        // Cliente 6 ESTANDAR sin NIT; producto 3 (10000) x 25 = 250000, 25 unidades.
        ResultadoPedido r = gestor.procesarPedido(
            pedido(6L, "vol@tienda.com", new ItemPedido(3L, 25)));
        assertTrue(r.isConfirmado());
        assertEquals(total(250000, 0.12), r.getTotal(), 0.001);
    }

    @Test
    void sinCampanaAplicableNoHayDescuentoParaClienteEstandar() {
        // Cliente 6 ESTANDAR sin NIT, pocas unidades, Black Friday inactivo -> 0%.
        ResultadoPedido r = gestor.procesarPedido(
            pedido(6L, "estandar@tienda.com", new ItemPedido(2L, 1)));
        assertTrue(r.isConfirmado());
        assertEquals(total(50000, 0.0), r.getTotal(), 0.001);
    }
}
