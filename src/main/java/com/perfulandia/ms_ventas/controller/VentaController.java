package com.perfulandia.ms_ventas.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.perfulandia.ms_ventas.model.Carrito;
import com.perfulandia.ms_ventas.model.Factura;
import com.perfulandia.ms_ventas.model.MetodoPago;
import com.perfulandia.ms_ventas.model.Venta;
import com.perfulandia.ms_ventas.service.VentaService;

@RestController
@RequestMapping("/api/v1")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @GetMapping("/carritos/{idUsuario}")
    public ResponseEntity<Carrito> obtenerCarritoActivo(@PathVariable Long idUsuario) {
        Carrito carrito = ventaService.obtenerCarritoActivo(idUsuario);
        return ResponseEntity.ok(carrito);
    }

    @PostMapping("/carritos/{idCarrito}/items")
    public ResponseEntity<Carrito> agregarItem(
            @PathVariable Long idCarrito,
            @RequestParam Long idProducto,
            @RequestParam int cantidad) {
        Carrito carrito = ventaService.agregarItem(idCarrito, idProducto, cantidad);
        return ResponseEntity.ok(carrito);
    }

    @DeleteMapping("/carritos/{idCarrito}/items/{idProducto}")
    public ResponseEntity<Carrito> eliminarItem(
            @PathVariable Long idCarrito,
            @PathVariable Long idProducto) {
        Carrito carrito = ventaService.eliminarItem(idCarrito, idProducto);
        return ResponseEntity.ok(carrito);
    }

    @PutMapping("/carritos/{idCarrito}/items/{idProducto}")
    public ResponseEntity<Carrito> actualizarCantidad(
            @PathVariable Long idCarrito,
            @PathVariable Long idProducto,
            @RequestParam int cantidad) {
        Carrito carrito = ventaService.actualizarCantidad(idCarrito, idProducto, cantidad);
        return ResponseEntity.ok(carrito);
    }

    @PutMapping("/carritos/{idCarrito}/cupon")
    public ResponseEntity<Carrito> aplicarCupon(
            @PathVariable Long idCarrito,
            @RequestParam String codigo) {
        Carrito carrito = ventaService.aplicarCupon(idCarrito, codigo);
        return ResponseEntity.ok(carrito);
    }

    @PostMapping("/ventas")
    public ResponseEntity<Venta> registrarVenta(
            @RequestParam Long idCarrito,
            @RequestParam MetodoPago metodoPago,
            @RequestParam String direccionEntrega) {
        Venta venta = ventaService.registrarVenta(idCarrito, metodoPago, direccionEntrega);
        return ResponseEntity.ok(venta);
    }

    @GetMapping("/ventas/{idVenta}")
    public ResponseEntity<Venta> consultarVenta(@PathVariable Long idVenta) {
        Venta venta = ventaService.consultarVenta(idVenta);
        return ResponseEntity.ok(venta);
    }

    @GetMapping("/ventas/historial/{idUsuario}")
    public ResponseEntity<List<Venta>> listarHistorial(@PathVariable Long idUsuario) {
        List<Venta> historial = ventaService.listarHistorial(idUsuario);
        return ResponseEntity.ok(historial);
    }

    @PostMapping("/facturas/{idVenta}")
    public ResponseEntity<Factura> emitirFactura(@PathVariable Long idVenta) {
        Factura factura = ventaService.emitirFactura(idVenta);
        return ResponseEntity.ok(factura);
    }

    @PostMapping("/facturas/{idFactura}/email")
    public ResponseEntity<String> enviarFacturaPorEmail(
            @PathVariable Long idFactura,
            @RequestParam String email) {
        String confirmacion = ventaService.enviarFacturaPorEmail(idFactura, email);
        return ResponseEntity.ok(confirmacion);
    }
}