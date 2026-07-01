package com.perfulandia.ms_ventas.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.perfulandia.ms_ventas.model.Carrito;
import com.perfulandia.ms_ventas.model.EstadoCarrito;
import com.perfulandia.ms_ventas.model.EstadoPago;
import com.perfulandia.ms_ventas.model.EstadoVenta;
import com.perfulandia.ms_ventas.model.Factura;
import com.perfulandia.ms_ventas.model.ItemCarrito;
import com.perfulandia.ms_ventas.model.ItemInventarioDTO;
import com.perfulandia.ms_ventas.model.MetodoPago;
import com.perfulandia.ms_ventas.model.ProductoDTO;
import com.perfulandia.ms_ventas.model.UsuarioDTO;
import com.perfulandia.ms_ventas.model.Venta;
import com.perfulandia.ms_ventas.repository.CarritoRepository;
import com.perfulandia.ms_ventas.repository.FacturaRepository;
import com.perfulandia.ms_ventas.repository.ItemCarritoRepository;
import com.perfulandia.ms_ventas.repository.VentaRepository;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private CarritoRepository carritoRepository;
    @Mock private ItemCarritoRepository itemCarritoRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private VentaService ventaService;

    @Test
    void testObtenerCarritoActivo_existente() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findByIdUsuarioAndEstado(1L, EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carrito));

        // When
        Carrito resultado = ventaService.obtenerCarritoActivo(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoCarrito.ACTIVO, resultado.getEstado());
        verify(carritoRepository, never()).save(any(Carrito.class));
    }

    @Test
    void testObtenerCarritoActivo_noExiste_creaNuevo() {
        // Given
        Carrito nuevo = new Carrito(1L, 1L, null, 0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findByIdUsuarioAndEstado(1L, EstadoCarrito.ACTIVO))
                .thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenReturn(nuevo);

        // When
        Carrito resultado = ventaService.obtenerCarritoActivo(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(EstadoCarrito.ACTIVO, resultado.getEstado());
        assertEquals(0, resultado.getSubtotal());
        verify(carritoRepository, times(1)).save(any(Carrito.class));
    }

    @Test
    void testAgregarItem_exitoso_itemNuevo() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        ProductoDTO producto = new ProductoDTO(1L, "Perfume X", 50.0);
        ItemInventarioDTO inventario = new ItemInventarioDTO(1L, 10);

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(restTemplate.getForObject(anyString(), eq(ProductoDTO.class))).thenReturn(producto);
        when(restTemplate.getForObject(anyString(), eq(ItemInventarioDTO.class))).thenReturn(inventario);
        when(itemCarritoRepository.findByCarrito_IdAndIdProducto(1L, 1L)).thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        // When
        Carrito resultado = ventaService.agregarItem(1L, 1L, 2);

        // Then
        assertNotNull(resultado);
        assertEquals(1, resultado.getItems().size());
        assertEquals(50.0, resultado.getItems().get(0).getPrecioUnitario());
        assertEquals(100.0, resultado.getSubtotal());
        verify(carritoRepository, times(1)).save(any(Carrito.class));
    }

    @Test
    void testAgregarItem_productoYaExiste_sumaCantidad() {
        // Given
        ItemCarrito itemExistente = new ItemCarrito(1L, 1L, 3, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(itemExistente);
        Carrito carrito = new Carrito(1L, 1L, null, 150.0, 0, EstadoCarrito.ACTIVO, items);
        itemExistente.setCarrito(carrito);

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(restTemplate.getForObject(anyString(), eq(ProductoDTO.class)))
                .thenReturn(new ProductoDTO(1L, "Perfume X", 50.0));
        when(restTemplate.getForObject(anyString(), eq(ItemInventarioDTO.class)))
                .thenReturn(new ItemInventarioDTO(1L, 10));
        when(itemCarritoRepository.findByCarrito_IdAndIdProducto(1L, 1L))
                .thenReturn(Optional.of(itemExistente));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        // When
        Carrito resultado = ventaService.agregarItem(1L, 1L, 2);

        // Then
        assertEquals(5, itemExistente.getCantidad());
        assertEquals(250.0, resultado.getSubtotal());
    }

    @Test
    void testAgregarItem_cantidadInvalida_lanzaError() {
        // Given (sin mocks)

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.agregarItem(1L, 1L, 0));

        // Then
        assertEquals("La cantidad debe ser mayor a 0", ex.getMessage());
        verify(carritoRepository, never()).findById(any());
    }

    @Test
    void testAgregarItem_productoNoExiste_lanzaError() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(restTemplate.getForObject(anyString(), eq(ProductoDTO.class))).thenReturn(null);

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.agregarItem(1L, 1L, 2));

        // Then
        assertEquals("El producto no existe en el catálogo", ex.getMessage());
    }

    @Test
    void testAgregarItem_sinStock_lanzaError() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(restTemplate.getForObject(anyString(), eq(ProductoDTO.class)))
                .thenReturn(new ProductoDTO(1L, "Perfume X", 50.0));
        when(restTemplate.getForObject(anyString(), eq(ItemInventarioDTO.class)))
                .thenReturn(new ItemInventarioDTO(1L, 1));
        when(itemCarritoRepository.findByCarrito_IdAndIdProducto(1L, 1L)).thenReturn(Optional.empty());

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.agregarItem(1L, 1L, 5));

        // Then
        assertEquals("Stock insuficiente para el producto", ex.getMessage());
    }

    @Test
    void testEliminarItem_exitoso() {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 2, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.ACTIVO, items);
        item.setCarrito(carrito);

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByCarrito_IdAndIdProducto(1L, 1L)).thenReturn(Optional.of(item));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        // When
        Carrito resultado = ventaService.eliminarItem(1L, 1L);

        // Then
        assertEquals(0, resultado.getItems().size());
        assertEquals(0.0, resultado.getSubtotal());
    }

    @Test
    void testEliminarItem_noExiste_lanzaError() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByCarrito_IdAndIdProducto(1L, 99L)).thenReturn(Optional.empty());

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.eliminarItem(1L, 99L));

        // Then
        assertEquals("No existe el producto en el carrito", ex.getMessage());
    }

    @Test
    void testActualizarCantidad_exitoso() {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 2, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.ACTIVO, items);
        item.setCarrito(carrito);

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByCarrito_IdAndIdProducto(1L, 1L)).thenReturn(Optional.of(item));
        when(restTemplate.getForObject(anyString(), eq(ItemInventarioDTO.class)))
                .thenReturn(new ItemInventarioDTO(1L, 10));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        // When
        Carrito resultado = ventaService.actualizarCantidad(1L, 1L, 5);

        // Then
        assertEquals(5, item.getCantidad());
        assertEquals(250.0, resultado.getSubtotal());
    }

    @Test
    void testAplicarCupon_valido() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        // When
        Carrito resultado = ventaService.aplicarCupon(1L, "DESCUENTO10");

        // Then
        assertEquals("DESCUENTO10", resultado.getCuponAplicado());
        assertEquals(10.0, resultado.getDescuento());
    }

    @Test
    void testAplicarCupon_invalido() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.aplicarCupon(1L, "INVALIDO"));

        // Then
        assertEquals("Cupón inválido", ex.getMessage());
    }

    @Test
    void testRegistrarVenta_exitoso() {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 2, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 10.0, EstadoCarrito.ACTIVO, items);
        item.setCarrito(carrito);

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(restTemplate.getForObject(anyString(), eq(UsuarioDTO.class)))
                .thenReturn(new UsuarioDTO(1L, "Ana", "ana@test.com"));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        // When
        Venta resultado = ventaService.registrarVenta(1L, MetodoPago.TARJETA, "Calle 1");

        // Then
        assertEquals(EstadoVenta.COMPLETADA, resultado.getEstadoVenta());
        assertEquals(EstadoPago.PAGADO, resultado.getEstadoPago());
        assertEquals(90.0, resultado.getTotal());
        assertEquals(EstadoCarrito.COMPLETADO, carrito.getEstado());
        verify(facturaRepository, times(1)).save(any(Factura.class));
    }

    @Test
    void testRegistrarVenta_carritoVacio_lanzaError() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0, 0, EstadoCarrito.ACTIVO, new ArrayList<>());
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.registrarVenta(1L, MetodoPago.EFECTIVO, "Calle 1"));

        // Then
        assertEquals("El carrito no tiene productos", ex.getMessage());
    }

    @Test
    void testRegistrarVenta_carritoCompletado_lanzaError() {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.COMPLETADO, new ArrayList<>());
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.registrarVenta(1L, MetodoPago.EFECTIVO, "Calle 1"));

        // Then
        assertEquals("El carrito no está activo", ex.getMessage());
    }

    @Test
    void testRegistrarVenta_metodoPagoNull_lanzaError() {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 2, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.ACTIVO, items);
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.registrarVenta(1L, null, "Calle 1"));

        // Then
        assertEquals("El método de pago es obligatorio", ex.getMessage());
    }

    @Test
    void testConsultarVenta_existente() {
        // Given
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstadoVenta(EstadoVenta.COMPLETADA);
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        // When
        Venta resultado = ventaService.consultarVenta(1L);

        // Then
        assertEquals(EstadoVenta.COMPLETADA, resultado.getEstadoVenta());
    }

    @Test
    void testConsultarVenta_noExiste() {
        // Given
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.consultarVenta(99L));

        // Then
        assertEquals("No existe la venta", ex.getMessage());
    }

    @Test
    void testListarHistorial() {
        // Given
        Venta v1 = new Venta(); v1.setId(1L); v1.setIdUsuario(1L);
        Venta v2 = new Venta(); v2.setId(2L); v2.setIdUsuario(1L);
        when(ventaRepository.findByIdUsuario(1L)).thenReturn(Arrays.asList(v1, v2));

        // When
        List<Venta> resultado = ventaService.listarHistorial(1L);

        // Then
        assertEquals(2, resultado.size());
    }

    @Test
    void testEmitirFactura_existente() {
        // Given
        Factura facturaExistente = new Factura(1L, 1L, "FAC-001", 100.0, LocalDateTime.now());
        when(facturaRepository.findByIdVenta(1L)).thenReturn(Optional.of(facturaExistente));

        // When
        Factura resultado = ventaService.emitirFactura(1L);

        // Then
        assertEquals("FAC-001", resultado.getNumeroFactura());
        verify(facturaRepository, never()).save(any(Factura.class));
    }

    @Test
    void testEmitirFactura_noExiste_creaFactura() {
        // Given
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstadoVenta(EstadoVenta.COMPLETADA);
        venta.setTotal(100.0);
        when(facturaRepository.findByIdVenta(1L)).thenReturn(Optional.empty());
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        // When
        Factura resultado = ventaService.emitirFactura(1L);

        // Then
        assertTrue(resultado.getNumeroFactura().startsWith("FAC-"));
        assertEquals(100.0, resultado.getMontoTotal());
    }

    @Test
    void testEnviarFacturaPorEmail_exitoso() {
        // Given
        Factura factura = new Factura(1L, 1L, "FAC-001", 100.0, LocalDateTime.now());
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

        // When
        String resultado = ventaService.enviarFacturaPorEmail(1L, "cliente@test.com");

        // Then
        assertTrue(resultado.contains("FAC-001"));
        assertTrue(resultado.contains("cliente@test.com"));
    }

    @Test
    void testEnviarFacturaPorEmail_emailVacio_lanzaError() {
        // Given
        Factura factura = new Factura(1L, 1L, "FAC-001", 100.0, LocalDateTime.now());
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.enviarFacturaPorEmail(1L, ""));

        // Then
        assertEquals("El email no puede estar vacío", ex.getMessage());
    }

    @Test
    void testEnviarFacturaPorEmail_emailNull_lanzaError() {
        // Given
        Factura factura = new Factura(1L, 1L, "FAC-001", 100.0, LocalDateTime.now());
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.enviarFacturaPorEmail(1L, null));

        // Then
        assertEquals("El email no puede estar vacío", ex.getMessage());
    }

    @Test
    void testActualizarCantidad_sinStock_lanzaError() {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 2, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.ACTIVO, items);
        item.setCarrito(carrito);

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByCarrito_IdAndIdProducto(1L, 1L)).thenReturn(Optional.of(item));
        when(restTemplate.getForObject(anyString(), eq(ItemInventarioDTO.class)))
                .thenReturn(new ItemInventarioDTO(1L, 3));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.actualizarCantidad(1L, 1L, 10));

        // Then
        assertEquals("Stock insuficiente para la cantidad solicitada", ex.getMessage());
    }

    @Test
    void testEmitirFactura_ventaNoCompletada_lanzaError() {
        // Given
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstadoVenta(EstadoVenta.PENDIENTE);
        when(facturaRepository.findByIdVenta(1L)).thenReturn(Optional.empty());
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.emitirFactura(1L));

        // Then
        assertEquals("La venta no está completada", ex.getMessage());
    }

    @Test
    void testEnviarFacturaPorEmail_facturaNoExiste_lanzaError() {
        // Given
        when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.enviarFacturaPorEmail(99L, "test@test.com"));

        // Then
        assertEquals("No existe la factura", ex.getMessage());
    }

    @Test
    void testRegistrarVenta_usuarioNoExiste_lanzaError() {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 2, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0, EstadoCarrito.ACTIVO, items);
        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));
        when(restTemplate.getForObject(anyString(), eq(UsuarioDTO.class))).thenReturn(null);

        // When
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ventaService.registrarVenta(1L, MetodoPago.EFECTIVO, "Calle 1"));

        // Then
        assertEquals("El usuario no existe", ex.getMessage());
        verify(ventaRepository, never()).save(any(Venta.class));
    }
}