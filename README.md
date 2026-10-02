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

### Parte 2 — Crecimiento del proyecto

**Antipatrón identificado: Golden Hammer.**

Dos semanas después, mercadeo pidió tres campañas: `BLACK_FRIDAY` (25% fijo),
`CORPORATIVO` (10% a clientes con NIT) y `VOLUMEN` (12% si el pedido supera 20
unidades). Se resolvieron **agregando tres eslabones más a la cadena de
validación** (`PromocionBlackFriday`, `PromocionCorporativo`,
`PromocionVolumen` extendiendo `ValidadorPedido`), reutilizando Chain of
Responsibility solo porque "ya funcionó" en la Parte 1 — sin evaluar si el
nuevo problema tenía esa forma.

Evidencia de que **no** corresponde una cadena:

- **No hay dependencia de orden** entre las tres campañas ni entre ellas y
  `ValidadorStock`/`ValidadorCliente`: ejecutar `PromocionVolumen` antes o
  después de `PromocionCorporativo` da el mismo resultado. En cambio
  `ValidadorStock` sí debe ir antes que `ValidadorCliente`. La propiedad que
  justificaba la cadena (orden + corte anticipado) está ausente aquí.
- **Ninguna de las tres rechaza nada**: `ValidadorPedido`, cuyo contrato es
  "decidir si el pedido continúa o se rechaza", ahora contiene clases que solo
  escriben en un campo compartido (`descuentoCampana`) y nunca cortan el flujo.
- **El campo mutable compartido genera ambigüedad**: si dos campañas debieran
  combinarse (sumar en vez de tomar el máximo), la cadena no lo expresa sin
  inventar reglas sobre quién sobrescribe a quién en `aplicarDescuentoCampana`.

Esto es **Golden Hammer**: aplicar una herramienta conocida a un problema con
una forma distinta, en vez de evaluar cuál es la apropiada.

**Patrón aplicado (corrección): Strategy.** Las tres campañas tienen la misma
forma que `DescuentoVip`/`DescuentoFrecuente` —calculan un porcentaje sin
depender de un orden—, así que se modelan como `EstrategiaDescuento`
(`DescuentoBlackFriday`, `DescuentoCorporativo`, `DescuentoVolumen`) y se
evalúan en `CalculadorDescuentoFinal`, que combina el descuento por tipo de
cliente con el mayor de las campañas (`Math.max`), la misma regla de negocio
que antes pero sin un campo mutable escrito por clases que no validan nada.
*Alternativa descartada:* mantenerlas como eslabones de la cadena — descartada
explícitamente por ser la causa del antipatrón.

Los tres eslabones `Promocion*` y el campo `descuentoCampana` se **eliminan por
completo** (no se comentan): dejar código comentado "por si acaso" es el
mecanismo por el que nace un **Lava Flow**; el historial de Git es el lugar
correcto para conservar esa referencia. `ValidadorPedido` vuelve a contener
únicamente `ValidadorStock` y `ValidadorCliente`.

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
