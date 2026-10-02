-- Clientes: un cliente por cada ruta de prueba
INSERT INTO clientes (id, tipo_cliente, nit) VALUES
  (1, 'VIP', NULL),            -- descuento VIP por tramos de subtotal
  (2, 'FRECUENTE', NULL),      -- descuento por numero de pedidos previos
  (3, 'MOROSO', NULL),         -- moroso CON deuda pendiente
  (4, 'MOROSO', NULL),         -- moroso SIN deuda (se permite el pedido)
  (5, 'ESTANDAR', '900123456-7'), -- cliente con NIT -> campana CORPORATIVO
  (6, 'ESTANDAR', NULL);       -- cliente estandar sin NIT

-- Productos y precios
INSERT INTO productos (id, precio) VALUES
  (1, 100000),
  (2, 50000),
  (3, 10000),
  (4, 200000);

-- Inventario: el producto 4 tiene stock bajo para la ruta "stock insuficiente"
INSERT INTO inventario (producto_id, stock) VALUES
  (1, 1000),
  (2, 1000),
  (3, 1000),
  (4, 2);

-- Factura pendiente del cliente moroso 3 (genera deuda)
INSERT INTO facturas (cliente_id, monto, pagada) VALUES
  (3, 50000, FALSE),
  (4, 30000, TRUE);

-- Historial de pedidos del cliente FRECUENTE 2 (11 pedidos previos -> > 10 -> 8%)
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO'),
  (2, 0, 0, 0, 0, CURRENT_TIMESTAMP, 'CONFIRMADO');
