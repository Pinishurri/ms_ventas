package com.perfulandia.ms_ventas.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.perfulandia.ms_ventas.model.Carrito;
import com.perfulandia.ms_ventas.model.EstadoCarrito;
import com.perfulandia.ms_ventas.model.EstadoPago;
import com.perfulandia.ms_ventas.model.EstadoVenta;
import com.perfulandia.ms_ventas.model.Factura;
import com.perfulandia.ms_ventas.model.ItemCarrito;
import com.perfulandia.ms_ventas.model.MetodoPago;
import com.perfulandia.ms_ventas.model.Venta;
import com.perfulandia.ms_ventas.service.VentaService;

@WebMvcTest(VentaController.class)
@ActiveProfiles("test")
public class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentaService ventaService;

    @Test
    void testObtenerCarritoActivo_exitoso() throws Exception {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0.0, 0.0, EstadoCarrito.ACTIVO, new ArrayList<>());
        Mockito.when(ventaService.obtenerCarritoActivo(1L)).thenReturn(carrito);

        // When - Then
        mockMvc.perform(get("/api/v1/carritos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void testObtenerCarritoActivo_error() throws Exception {
        // Given
        Mockito.when(ventaService.obtenerCarritoActivo(99L))
                .thenThrow(new RuntimeException("No existe el carrito"));

        // When - Then
        mockMvc.perform(get("/api/v1/carritos/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAgregarItem_exitoso() throws Exception {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 2, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 100.0, 0.0, EstadoCarrito.ACTIVO, items);
        Mockito.when(ventaService.agregarItem(1L, 1L, 2)).thenReturn(carrito);

        // When - Then
        mockMvc.perform(post("/api/v1/carritos/1/items")
                        .param("idProducto", "1")
                        .param("cantidad", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(100.0));
    }

    @Test
    void testAgregarItem_error() throws Exception {
        // Given
        Mockito.when(ventaService.agregarItem(1L, 99L, 2))
                .thenThrow(new RuntimeException("El producto no existe en el catálogo"));

        // When - Then
        mockMvc.perform(post("/api/v1/carritos/1/items")
                        .param("idProducto", "99")
                        .param("cantidad", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEliminarItem_exitoso() throws Exception {
        // Given
        Carrito carrito = new Carrito(1L, 1L, null, 0.0, 0.0, EstadoCarrito.ACTIVO, new ArrayList<>());
        Mockito.when(ventaService.eliminarItem(1L, 1L)).thenReturn(carrito);

        // When - Then
        mockMvc.perform(delete("/api/v1/carritos/1/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(0.0));
    }

    @Test
    void testActualizarCantidad_exitoso() throws Exception {
        // Given
        ItemCarrito item = new ItemCarrito(1L, 1L, 5, 50.0, null);
        List<ItemCarrito> items = new ArrayList<>();
        items.add(item);
        Carrito carrito = new Carrito(1L, 1L, null, 250.0, 0.0, EstadoCarrito.ACTIVO, items);
        Mockito.when(ventaService.actualizarCantidad(1L, 1L, 5)).thenReturn(carrito);

        // When - Then
        mockMvc.perform(put("/api/v1/carritos/1/items/1")
                        .param("cantidad", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(250.0));
    }

    @Test
    void testAplicarCupon_exitoso() throws Exception {
        // Given
        Carrito carrito = new Carrito(1L, 1L, "DESCUENTO10", 100.0, 10.0, EstadoCarrito.ACTIVO, new ArrayList<>());
        Mockito.when(ventaService.aplicarCupon(1L, "DESCUENTO10")).thenReturn(carrito);

        // When - Then
        mockMvc.perform(put("/api/v1/carritos/1/cupon")
                        .param("codigo", "DESCUENTO10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuponAplicado").value("DESCUENTO10"))
                .andExpect(jsonPath("$.descuento").value(10.0));
    }

    @Test
    void testRegistrarVenta_exitoso() throws Exception {
        // Given
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstadoVenta(EstadoVenta.COMPLETADA);
        venta.setEstadoPago(EstadoPago.PAGADO);
        venta.setMetodoPago(MetodoPago.TARJETA);
        venta.setTotal(90.0);
        Mockito.when(ventaService.registrarVenta(1L, MetodoPago.TARJETA, "Calle 1")).thenReturn(venta);

        // When - Then
        mockMvc.perform(post("/api/v1/ventas")
                        .param("idCarrito", "1")
                        .param("metodoPago", "TARJETA")
                        .param("direccionEntrega", "Calle 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVenta").value("COMPLETADA"))
                .andExpect(jsonPath("$.estadoPago").value("PAGADO"))
                .andExpect(jsonPath("$.total").value(90.0));
    }

    @Test
    void testRegistrarVenta_error() throws Exception {
        // Given
        Mockito.when(ventaService.registrarVenta(1L, MetodoPago.EFECTIVO, "Calle 1"))
                .thenThrow(new RuntimeException("El carrito no tiene productos"));

        // When - Then
        mockMvc.perform(post("/api/v1/ventas")
                        .param("idCarrito", "1")
                        .param("metodoPago", "EFECTIVO")
                        .param("direccionEntrega", "Calle 1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConsultarVenta_exitoso() throws Exception {
        // Given
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setEstadoVenta(EstadoVenta.COMPLETADA);
        Mockito.when(ventaService.consultarVenta(1L)).thenReturn(venta);

        // When - Then
        mockMvc.perform(get("/api/v1/ventas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estadoVenta").value("COMPLETADA"));
    }

    @Test
    void testConsultarVenta_error() throws Exception {
        // Given
        Mockito.when(ventaService.consultarVenta(99L))
                .thenThrow(new RuntimeException("No existe la venta"));

        // When - Then
        mockMvc.perform(get("/api/v1/ventas/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testListarHistorial_exitoso() throws Exception {
        // Given
        Venta v1 = new Venta(); v1.setId(1L); v1.setIdUsuario(1L);
        Venta v2 = new Venta(); v2.setId(2L); v2.setIdUsuario(1L);
        Mockito.when(ventaService.listarHistorial(1L)).thenReturn(Arrays.asList(v1, v2));

        // When - Then
        mockMvc.perform(get("/api/v1/ventas/historial/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    void testEmitirFactura_exitoso() throws Exception {
        // Given
        Factura factura = new Factura(1L, 1L, "FAC-001", 90.0, null);
        Mockito.when(ventaService.emitirFactura(1L)).thenReturn(factura);

        // When - Then
        mockMvc.perform(post("/api/v1/facturas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFactura").value("FAC-001"))
                .andExpect(jsonPath("$.montoTotal").value(90.0));
    }

    @Test
    void testEmitirFactura_error() throws Exception {
        // Given
        Mockito.when(ventaService.emitirFactura(99L))
                .thenThrow(new RuntimeException("No existe la venta"));

        // When - Then
        mockMvc.perform(post("/api/v1/facturas/99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEnviarFacturaPorEmail_exitoso() throws Exception {
        // Given
        Mockito.when(ventaService.enviarFacturaPorEmail(1L, "cliente@test.com"))
                .thenReturn("Factura FAC-001 enviada a cliente@test.com");

        // When - Then
        mockMvc.perform(post("/api/v1/facturas/1/email")
                        .param("email", "cliente@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Factura FAC-001 enviada a cliente@test.com"));
    }

    @Test
    void testEnviarFacturaPorEmail_error() throws Exception {
        // Given
        Mockito.when(ventaService.enviarFacturaPorEmail(1L, ""))
                .thenThrow(new RuntimeException("El email no puede estar vacío"));

        // When - Then
        mockMvc.perform(post("/api/v1/facturas/1/email")
                        .param("email", ""))
                .andExpect(status().isBadRequest());
    }
}