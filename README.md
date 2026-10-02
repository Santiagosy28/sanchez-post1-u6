# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción

Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño
de Software — Sexto Semestre. Un único proyecto Spring Boot
(`pedidos-service/`) con dos partes: diagnóstico y refactorización de
un antipatrón combinado en `GestorPedidos`, y diagnóstico y corrección
de un segundo antipatrón introducido al hacer crecer el mismo proyecto
con tres campañas de descuento.

## Decisiones de diseño

### Parte 1 — GestorPedidos

**Antipatrón identificado: God Object + Spaghetti Code combinados.**

`GestorPedidos.procesarPedido()` concentraba, en un único método de más de
100 líneas dentro de una clase de 340 líneas (con seis métodos privados
auxiliares: `obtenerHistorialCliente`, `formatearFactura`,
`calcularImpuestoRegional`, `reintentarNotificacion`, `purgarPedidosVencidos`,
`construirCuerpoCorreo`), **seis responsabilidades distintas**, cada una una
razón diferente para cambiar la clase (viola SRP):

| # | Responsabilidad | Evidencia en el método original |
|---|-----------------|---------------------------------|
| 1 | Validación de stock | bloque `// Validacion de stock`: `SELECT stock FROM inventario` dentro de un `for` sobre los items |
| 2 | Validación de cliente y mora | bloque `// Validacion de cliente y mora`: `SELECT tipo_cliente`, `SELECT SUM(monto) FROM facturas`, con una excepción por horario (`LocalTime.now()`) |
| 3 | Cálculo de subtotal | bloque `// Calculo de subtotal`: un `SELECT precio FROM productos` por item |
| 4 | Cálculo de descuento | bloque `// Calculo de descuento`: `if/else` anidado por tipo de cliente y monto, con **hasta 3 niveles de anidamiento** (VIP → tramo de subtotal → valor) |
| 5 | Persistencia | bloque `// Persistencia directa via JDBC`: `INSERT INTO pedidos`, `CALL IDENTITY()`, `INSERT INTO detalle_pedido`, `UPDATE inventario`, sin repositorio ni transacción |
| 6 | Notificación | bloque `// Notificacion`: construcción del `StringBuilder` del cuerpo del correo y `emailService.enviar(...)` embebidos |

**Spaghetti Code / mezcla de niveles de abstracción:** el mismo método opera
a la vez con SQL embebido (strings `"SELECT ... FROM ..."`), reglas de negocio
(tramos de descuento), formato de texto del correo y registro (`log`). Un
cambio en las reglas de descuento obliga a releer el método completo, y
**agregar un nuevo tipo de cliente** con reglas propias exige modificar el
bloque 4 (el `if/else` por tipo) en medio del método, con riesgo de romper
las demás responsabilidades.

**Patrón aplicado:**

- **Chain of Responsibility** para la secuencia de validaciones
  (`ValidadorPedido` → `ValidadorStock` → `ValidadorCliente`). Se eligió
  porque las validaciones **tienen una dependencia real de orden y de corte
  anticipado**: si `ValidadorStock` rechaza el pedido, `ValidadorCliente` no
  debe ejecutarse.
  *Alternativa descartada:* una lista de `Predicate<ContextoPedido>` evaluada
  en orden. Se descartó porque evaluaría todos los predicados aunque el primero
  ya hubiera fallado y no permite que un validador decida **no delegar** al
  siguiente — el corte anticipado que sí ofrece la cadena.

- **Strategy** para el cálculo de descuento por tipo de cliente
  (`EstrategiaDescuento` con `DescuentoVip`, `DescuentoFrecuente`,
  `DescuentoEstandar`, seleccionadas por `SelectorEstrategiaDescuento`).
  Se eligió porque las reglas de descuento **no dependen de un orden** entre
  sí: siempre se aplica exactamente una, según el tipo de cliente.
  *Alternativa descartada:* un eslabón más de la cadena de validación. Se
  descartó porque modelar el descuento como cadena obligaría a un mecanismo
  artificial para garantizar que solo un eslabón module el valor, cuando un
  mapa de selección directa lo resuelve con menos indirección y sin
  condicionales anidados.

La **persistencia** se extrajo a `PedidoRepository` y la **notificación** a
`NotificacionPedidoService`, dejando `GestorPedidos` como un orquestador
delgado que coordina las cuatro capas (validación, descuento, persistencia,
notificación) sin conocer sus detalles internos.

## Cómo ejecutar

```
$ mvn spring-boot:run
$ mvn test
```

> **Nota de entorno.** El proyecto corre contra H2 en memoria. La URL usa
> `MODE=LEGACY` para restaurar la función `CALL IDENTITY()` (removida en H2
> 2.x) que usa el código dado, y el pool Hikari se fija en 1 conexión para que
> `CALL IDENTITY()` (función de sesión) devuelva el id recién insertado.

## Herramientas utilizadas

- Java 17, Spring Boot, Spring JDBC, Maven, H2 Database
- VS Code / IntelliJ IDEA, Git, GitHub
