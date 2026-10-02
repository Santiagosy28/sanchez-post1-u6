package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

// Eslabon 2: existencia y mora del cliente (depende de que el eslabon anterior haya pasado)
@Component
public class ValidadorCliente extends ValidadorPedido {
    private final JdbcTemplate jdbcTemplate;

    public ValidadorCliente(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();
        // Lookup tolerante a cero filas para que la rama "cliente no registrado" sea alcanzable.
        String tipo = jdbcTemplate.query(
            "SELECT tipo_cliente FROM clientes WHERE id = ?",
            rs -> rs.next() ? rs.getString(1) : null, clienteId);
        if (tipo == null) { contexto.rechazar("Cliente no registrado"); return; }
        contexto.setTipoCliente(tipo);
        if (tipo.equals("MOROSO")) {
            Double deuda = jdbcTemplate.queryForObject(
                "SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false",
                Double.class, clienteId);
            boolean fueraDeHorarioDeCorte = !LocalTime.now().isBefore(LocalTime.of(20, 0));
            if (deuda != null && deuda > 0 && !fueraDeHorarioDeCorte) {
                contexto.rechazar("Cliente con deuda pendiente: $" + deuda);
            }
        }
    }
}
