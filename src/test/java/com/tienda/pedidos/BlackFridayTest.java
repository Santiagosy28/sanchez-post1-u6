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

// Contexto con la campana Black Friday ACTIVA.
@SpringBootTest(properties = "promo.black-friday.activa=true")
class BlackFridayTest {

    @Autowired
    private GestorPedidos gestor;

    @Test
    void campanaBlackFridayActivaAplicaVeinticincoPorCiento() {
        // Cliente 6 ESTANDAR sin NIT; producto 3 (10000) x 1 = 10000.
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(6L, "bf@tienda.com", List.of(new ItemPedido(3L, 1))));
        assertTrue(r.isConfirmado());
        double base = 10000 - 10000 * 0.25;
        assertEquals(base + base * 0.19, r.getTotal(), 0.001);
    }
}
