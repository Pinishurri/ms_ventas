package com.perfulandia.ms_ventas.controller;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.client.RestTemplate;

import com.perfulandia.ms_ventas.model.Carrito;
import com.perfulandia.ms_ventas.model.EstadoCarrito;
import com.perfulandia.ms_ventas.model.EstadoPago;
import com.perfulandia.ms_ventas.model.EstadoVenta;
import com.perfulandia.ms_ventas.model.Factura;
import com.perfulandia.ms_ventas.model.ItemCarrito;
import com.perfulandia.ms_ventas.model.MetodoPago;
import com.perfulandia.ms_ventas.model.UsuarioDTO;
import com.perfulandia.ms_ventas.model.Venta;
import com.perfulandia.ms_ventas.repository.CarritoRepository;
import com.perfulandia.ms_ventas.repository.FacturaRepository;
import com.perfulandia.ms_ventas.repository.VentaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaControllerITTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CarritoRepository carritoRepository;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private FacturaRepository facturaRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @BeforeEach
    void cleanDb() {
        facturaRepository.deleteAll();
        ventaRepository.deleteAll();
        carritoRepository.deleteAll();
    }

    @Test
    void testFlujoCompletoVenta() throws Exception {
        // Given
        Carrito carrito = new Carrito();
        carrito.setIdUsuario(1L);
        carrito.setEstado(EstadoCarrito.ACTIVO);
        carrito.setSubtotal(100.0);
        carrito.setDescuento(0.0);

        ItemCarrito item = new ItemCarrito();
        item.setIdProducto(1L);
        item.setCantidad(2);
        item.setPrecioUnitario(50.0);
        item.setCarrito(carrito);
        carrito.getItems().add(item);

        Carrito saved = carritoRepository.save(carrito);

        when(restTemplate.getForObject(anyString(), eq(UsuarioDTO.class)))
                .thenReturn(new UsuarioDTO(1L, "Ana", "ana@test.com"));

        // When - Then
        mockMvc.perform(post("/api/v1/ventas")
                        .param("idCarrito", String.valueOf(saved.getId()))
                        .param("metodoPago", "TARJETA")
                        .param("direccionEntrega", "Calle 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoVenta").value("COMPLETADA"))
                .andExpect(jsonPath("$.estadoPago").value("PAGADO"));

        List<Factura> facturas = facturaRepository.findAll();
        assertFalse(facturas.isEmpty());
        assertEquals(1, facturas.size());

        Carrito actualizado = carritoRepository.findById(saved.getId()).orElseThrow();
        assertEquals(EstadoCarrito.COMPLETADO, actualizado.getEstado());
    }

    @Test
    void testObtenerCarritoActivo_creaNuevo() throws Exception {
        // Given (sin carrito en H2)

        // When - Then
        mockMvc.perform(get("/api/v1/carritos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.idUsuario").value(1));

        assertEquals(1, carritoRepository.findAll().size());
    }

    @Test
    void testListarHistorialVacio() throws Exception {
        // Given (sin ventas en H2)

        // When - Then
        mockMvc.perform(get("/api/v1/ventas/historial/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testConsultarVentaNoExistente() throws Exception {
        // Given (sin venta 999 en H2)

        // When - Then (400 porque RuntimeException es manejada como Bad Request)
        mockMvc.perform(get("/api/v1/ventas/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEmitirFacturaExistente() throws Exception {
        // Given
        Venta venta = new Venta();
        venta.setIdUsuario(1L);
        venta.setEstadoVenta(EstadoVenta.COMPLETADA);
        venta.setEstadoPago(EstadoPago.PAGADO);
        venta.setMetodoPago(MetodoPago.TARJETA);
        venta.setTotal(100.0);
        venta.setFechaCreacion(LocalDateTime.now());
        Venta savedVenta = ventaRepository.save(venta);

        Factura factura = new Factura();
        factura.setIdVenta(savedVenta.getId());
        factura.setNumeroFactura("FAC-TEST-001");
        factura.setMontoTotal(100.0);
        factura.setFechaEmision(LocalDateTime.now());
        facturaRepository.save(factura);

        // When - Then
        mockMvc.perform(post("/api/v1/facturas/" + savedVenta.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFactura").value("FAC-TEST-001"));

        assertEquals(1, facturaRepository.findAll().size());
    }
}