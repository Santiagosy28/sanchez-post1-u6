package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.PromocionBlackFriday;
import com.tienda.pedidos.validacion.PromocionCorporativo;
import com.tienda.pedidos.validacion.PromocionVolumen;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// Orquestador delgado: coordina las cuatro capas (validacion, descuento,
// persistencia, notificacion) sin conocer sus detalles internos.
@Service
public class GestorPedidos {

    private final ValidadorPedido primerValidador;
    private final SelectorEstrategiaDescuento selector;
    private final PedidoRepository repository;
    private final NotificacionPedidoService notificacion;
    private final JdbcTemplate jdbcTemplate;

    public GestorPedidos(ValidadorStock stock, ValidadorCliente cliente,
                         PromocionBlackFriday blackFriday, PromocionCorporativo corporativo,
                         PromocionVolumen volumen, SelectorEstrategiaDescuento selector,
                         PedidoRepository repository,
                         NotificacionPedidoService notificacion, JdbcTemplate jdbcTemplate) {
        // Ahora la cadena encadena 5 eslabones: 2 de validacion real + 3 de "promocion".
        this.primerValidador = stock;
        stock.encadenar(cliente).encadenar(blackFriday).encadenar(corporativo).encadenar(volumen);
        this.selector = selector;
        this.repository = repository;
        this.notificacion = notificacion;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResultadoPedido procesarPedido(PedidoRequest request) {
        ContextoPedido contexto = new ContextoPedido(request);
        primerValidador.validar(contexto);
        if (contexto.isRechazado()) return ResultadoPedido.rechazado(contexto.getMotivoRechazo());

        double subtotal = calcularSubtotal(request); // consulta de precios extraida sin cambios de logica
        contexto.setSubtotal(subtotal);

        double descuentoTipoCliente = selector.seleccionar(contexto.getTipoCliente()).calcular(contexto);
        double descuento = Math.max(descuentoTipoCliente, contexto.getDescuentoCampana());
        double impuesto = (subtotal - subtotal * descuento) * 0.19;
        double total = subtotal - (subtotal * descuento) + impuesto;

        Long pedidoId = repository.guardar(contexto, descuento, impuesto, total);
        notificacion.notificarConfirmacion(contexto, pedidoId, descuento, impuesto, total);
        return ResultadoPedido.confirmado(pedidoId, total);
    }

    // Calculo puro del subtotal, con una unica consulta de precio por item.
    private double calcularSubtotal(PedidoRequest request) {
        double subtotal = 0;
        for (var item : request.getItems()) {
            Double precioUnitario = jdbcTemplate.queryForObject(
                "SELECT precio FROM productos WHERE id = ?", Double.class, item.getProductoId());
            subtotal += precioUnitario * item.getCantidad();
        }
        return subtotal;
    }
}
