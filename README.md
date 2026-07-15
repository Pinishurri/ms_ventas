# ms-ventas

## Descripción

Microservicio encargado del flujo de ventas de Perfulandia SPA. Gestiona carritos, productos dentro del carrito, registro de ventas, historial y facturas.

## Responsabilidad del microservicio

Este microservicio contiene el flujo principal de compra. Se comunica con `ms-usuarios`, `ms-catalogo` y `ms-inventario` para validar información externa antes de registrar una venta.

## Tecnologías usadas

- Java 25
- Spring Boot 4.0.7
- Maven / Packaging WAR
- Spring WebMVC
- Spring Data JPA + Hibernate
- MySQL para ejecución local
- H2 para pruebas
- Lombok
- Swagger con springdoc
- JUnit 5, Mockito y MockMvc
- RestTemplate para comunicación entre microservicios

## Puerto, base de datos y Swagger

| Elemento | Valor |
|---|---|
| Puerto | `8084` |
| Base de datos | `ventas_db` |
| Swagger UI | `http://localhost:8084/doc/swagger-ui.html` |
| Ruta base | `/api/v1` |

## Diagrama de clases

```text
┌──────────────────────────────┐        ┌──────────────────────────────┐
│            Carrito            │ 1    * │         ItemCarrito          │
├──────────────────────────────┤───────►├──────────────────────────────┤
│ id: Long                     │        │ id: Long                     │
│ idUsuario: Long              │        │ idProducto: Long             │
│ cuponAplicado: String        │        │ cantidad: int                │
│ subtotal: double             │        │ precioUnitario: double       │
│ descuento: double            │        │ carrito: Carrito             │
│ estado: EstadoCarrito        │        └──────────────────────────────┘
│ items: List<ItemCarrito>     │
└──────────────────────────────┘

┌──────────────────────────────┐        ┌──────────────────────────────┐
│             Venta             │ 1    * │           ItemVenta          │
├──────────────────────────────┤───────►├──────────────────────────────┤
│ id: Long                     │        │ id: Long                     │
│ idUsuario: Long              │        │ idProducto: Long             │
│ idSucursal: Long             │        │ cantidad: int                │
│ total: double                │        │ precioUnitario: double       │
│ estadoVenta: EstadoVenta     │        │ descuentoAplicado: double    │
│ metodoPago: MetodoPago       │        │ venta: Venta                 │
│ estadoPago: EstadoPago       │        └──────────────────────────────┘
│ direccionEntrega: String     │
│ fechaCreacion: LocalDateTime │
│ items: List<ItemVenta>       │
└──────────────────────────────┘

┌──────────────────────────────┐
│            Factura            │
├──────────────────────────────┤
│ id: Long                     │
│ idVenta: Long                │
│ numeroFactura: String        │
│ montoTotal: double           │
│ fechaEmision: LocalDateTime  │
└──────────────────────────────┘

DTOs usados para comunicación externa:
ProductoDTO | UsuarioDTO | ItemInventarioDTO

EstadoCarrito: ACTIVO | COMPLETADO
EstadoVenta: PENDIENTE | PROCESANDO | COMPLETADA
EstadoPago: PENDIENTE | PAGADO
MetodoPago: EFECTIVO | TARJETA | TRANSFERENCIA
```

## Estructura por capas

```text
controller  → Expone endpoints de carrito, ventas y facturas.
service     → Contiene el flujo de negocio de la venta.
repository  → Accede a carritos, ventas, items y facturas.
model       → Contiene entidades, enums y DTOs.
config      → Contiene RestTemplateConfig.
exception   → Manejo global de RuntimeException.
```

## Endpoints principales

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/v1/carritos/{idUsuario}` | Obtener o crear carrito activo |
| POST | `/api/v1/carritos/{idCarrito}/items?idProducto=...&cantidad=...` | Agregar item al carrito |
| DELETE | `/api/v1/carritos/{idCarrito}/items/{idProducto}` | Eliminar item del carrito |
| PUT | `/api/v1/carritos/{idCarrito}/items/{idProducto}?cantidad=...` | Actualizar cantidad |
| PUT | `/api/v1/carritos/{idCarrito}/cupon?codigo=...` | Aplicar cupón |
| POST | `/api/v1/ventas?idCarrito=...&metodoPago=...&direccionEntrega=...` | Registrar venta |
| GET | `/api/v1/ventas/{idVenta}` | Consultar venta |
| GET | `/api/v1/ventas/historial/{idUsuario}` | Listar historial de ventas |
| POST | `/api/v1/facturas/{idVenta}` | Emitir factura |
| POST | `/api/v1/facturas/{idFactura}/email?email=...` | Simular envío de factura por email |

## Reglas de negocio principales

- Cada usuario puede tener un carrito `ACTIVO`.
- Si el usuario no tiene carrito activo, se crea uno nuevo.
- Para agregar productos, la cantidad debe ser mayor a 0.
- Antes de agregar un producto, se valida que exista en catálogo.
- Antes de agregar o actualizar cantidad, se valida stock en inventario.
- El carrito debe estar `ACTIVO` para modificarlo.
- El cupón válido implementado es `DESCUENTO10`.
- Para registrar venta, el carrito debe tener productos.
- Para registrar venta, el método de pago es obligatorio.
- Al registrar una venta:
  - se valida el usuario,
  - se crea la venta,
  - se copian los `ItemCarrito` a `ItemVenta`,
  - se calcula el total,
  - el carrito pasa a `COMPLETADO`,
  - se genera una factura.
- Si ya existe factura para una venta, se retorna esa factura.

## Comunicación con otros microservicios

`ms-ventas` usa `RestTemplate` para consultar otros microservicios.

```text
ms-ventas → GET http://localhost:8081/api/v1/usuarios/{idUsuario}    → ms-usuarios
ms-ventas → GET http://localhost:8082/api/v1/productos/{idProducto}  → ms-catalogo
ms-ventas → GET http://localhost:8083/api/v1/inventario/{idProducto} → ms-inventario
```

Estas consultas se usan para:
- validar que el usuario exista,
- obtener datos del producto,
- verificar el stock disponible.

## Flujo principal de una venta

```text
1. Obtener carrito activo del usuario.
2. Agregar producto al carrito.
3. Consultar producto en ms-catalogo.
4. Consultar stock en ms-inventario.
5. Recalcular subtotal.
6. Aplicar cupón si corresponde.
7. Registrar venta.
8. Validar usuario en ms-usuarios.
9. Crear Venta.
10. Copiar ItemCarrito a ItemVenta.
11. Marcar carrito como COMPLETADO.
12. Crear Factura.
```

## Manejo de errores

El `GlobalExceptionHandler` captura `RuntimeException` y devuelve HTTP 400 con el mensaje en texto plano.

Ejemplo:

```text
El carrito no tiene productos
```

## Pruebas

| Clase | Qué prueba |
|---|---|
| `VentaServiceTest` | Lógica de carrito, agregar productos, stock, cupón, venta, factura y errores |
| `VentaControllerTest` | Endpoints del controller usando MockMvc y service simulado |
| `VentaControllerITTest` | Flujo de integración usando controller, service, repository y H2. El `RestTemplate` se mockea para no depender de otros MS levantados |
| `MsVentasApplicationTests` | Carga de contexto y ejecución del main |

## Ejecución local

Crear base de datos:

```sql
CREATE DATABASE ventas_db;
```

Ejecutar microservicio:

```bash
./mvnw spring-boot:run
```

Ejecutar pruebas:

```bash
./mvnw clean test
```

Swagger:

```text
http://localhost:8084/doc/swagger-ui.html
```

## Orden sugerido para probar flujo completo

```text
1. Levantar ms-usuarios en 8081.
2. Levantar ms-catalogo en 8082.
3. Levantar ms-inventario en 8083.
4. Levantar ms-ventas en 8084.
5. Crear usuario.
6. Crear producto.
7. Crear inventario para ese producto.
8. Obtener carrito activo.
9. Agregar producto al carrito.
10. Registrar venta.
11. Consultar venta o factura.
```
