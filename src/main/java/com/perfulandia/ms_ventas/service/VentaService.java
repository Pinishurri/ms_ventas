package com.perfulandia.ms_ventas.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.perfulandia.ms_ventas.model.Carrito;
import com.perfulandia.ms_ventas.model.EstadoCarrito;
import com.perfulandia.ms_ventas.model.EstadoPago;
import com.perfulandia.ms_ventas.model.EstadoVenta;
import com.perfulandia.ms_ventas.model.Factura;
import com.perfulandia.ms_ventas.model.ItemCarrito;
import com.perfulandia.ms_ventas.model.ItemInventarioDTO;
import com.perfulandia.ms_ventas.model.ItemVenta;
import com.perfulandia.ms_ventas.model.MetodoPago;
import com.perfulandia.ms_ventas.model.ProductoDTO;
import com.perfulandia.ms_ventas.model.UsuarioDTO;
import com.perfulandia.ms_ventas.model.Venta;
import com.perfulandia.ms_ventas.repository.CarritoRepository;
import com.perfulandia.ms_ventas.repository.FacturaRepository;
import com.perfulandia.ms_ventas.repository.ItemCarritoRepository;
import com.perfulandia.ms_ventas.repository.VentaRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class VentaService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String URL_USUARIOS   = "http://localhost:8081/api/v1/usuarios/";
    private static final String URL_CATALOGO   = "http://localhost:8082/api/v1/productos/";
    private static final String URL_INVENTARIO = "http://localhost:8083/api/v1/inventario/";

    public Carrito obtenerCarritoActivo(Long idUsuario) {
        return carritoRepository
                .findByIdUsuarioAndEstado(idUsuario, EstadoCarrito.ACTIVO)
                .orElseGet(() -> {
                    Carrito nuevo = new Carrito();
                    nuevo.setIdUsuario(idUsuario);
                    nuevo.setEstado(EstadoCarrito.ACTIVO);
                    nuevo.setSubtotal(0);
                    nuevo.setDescuento(0);
                    return carritoRepository.save(nuevo);
                });
    }

    public Carrito agregarItem(Long idCarrito, Long idProducto, int cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
        }

        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(() -> new RuntimeException("No existe el carrito"));

        validarCarritoActivo(carrito);

        ProductoDTO producto = obtenerProducto(idProducto);
        ItemInventarioDTO inventario = obtenerInventario(idProducto);

        Optional<ItemCarrito> itemExistente = itemCarritoRepository
                .findByCarrito_IdAndIdProducto(idCarrito, idProducto);

        if (itemExistente.isPresent()) {
            ItemCarrito item = itemExistente.get();
            int nuevaCantidad = item.getCantidad() + cantidad;
            if (inventario.getCantidadDisponible() < nuevaCantidad) {
                throw new RuntimeException("Stock insuficiente para la cantidad total solicitada");
            }
            item.setCantidad(nuevaCantidad);
        } else {
            if (inventario.getCantidadDisponible() < cantidad) {
                throw new RuntimeException("Stock insuficiente para el producto");
            }
            ItemCarrito nuevoItem = new ItemCarrito();
            nuevoItem.setIdProducto(idProducto);
            nuevoItem.setCantidad(cantidad);
            nuevoItem.setPrecioUnitario(producto.getPrecio());
            nuevoItem.setCarrito(carrito);
            carrito.getItems().add(nuevoItem);
        }

        recalcularSubtotal(carrito);
        return carritoRepository.save(carrito);
    }

    public Carrito eliminarItem(Long idCarrito, Long idProducto) {
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(() -> new RuntimeException("No existe el carrito"));

        validarCarritoActivo(carrito);

        ItemCarrito item = itemCarritoRepository
                .findByCarrito_IdAndIdProducto(idCarrito, idProducto)
                .orElseThrow(() -> new RuntimeException("No existe el producto en el carrito"));

        carrito.getItems().remove(item);
        recalcularSubtotal(carrito);
        return carritoRepository.save(carrito);
    }

    public Carrito actualizarCantidad(Long idCarrito, Long idProducto, int cantidad) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
        }

        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(() -> new RuntimeException("No existe el carrito"));

        validarCarritoActivo(carrito);

        ItemCarrito item = itemCarritoRepository
                .findByCarrito_IdAndIdProducto(idCarrito, idProducto)
                .orElseThrow(() -> new RuntimeException("No existe el producto en el carrito"));

        ItemInventarioDTO inventario = obtenerInventario(idProducto);
        if (inventario.getCantidadDisponible() < cantidad) {
            throw new RuntimeException("Stock insuficiente para la cantidad solicitada");
        }

        item.setCantidad(cantidad);
        recalcularSubtotal(carrito);
        return carritoRepository.save(carrito);
    }

    public Carrito aplicarCupon(Long idCarrito, String codigo) {
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(() -> new RuntimeException("No existe el carrito"));

        validarCarritoActivo(carrito);

        if (!"DESCUENTO10".equals(codigo)) {
            throw new RuntimeException("Cupón inválido");
        }

        carrito.setCuponAplicado(codigo);
        carrito.setDescuento(carrito.getSubtotal() * 0.10);
        return carritoRepository.save(carrito);
    }

    public Venta registrarVenta(Long idCarrito, MetodoPago metodoPago, String direccionEntrega) {
        Carrito carrito = carritoRepository.findById(idCarrito)
                .orElseThrow(() -> new RuntimeException("No existe el carrito"));

        validarCarritoActivo(carrito);

        if (carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito no tiene productos");
        }

        if (metodoPago == null) {
            throw new RuntimeException("El método de pago es obligatorio");
        }

        validarUsuario(carrito.getIdUsuario());

        Venta venta = new Venta();
        venta.setIdUsuario(carrito.getIdUsuario());
        venta.setEstadoVenta(EstadoVenta.COMPLETADA);
        venta.setEstadoPago(EstadoPago.PAGADO);
        venta.setMetodoPago(metodoPago);
        venta.setDireccionEntrega(direccionEntrega);
        venta.setFechaCreacion(LocalDateTime.now());

        for (ItemCarrito itemCarrito : carrito.getItems()) {
            ItemVenta itemVenta = new ItemVenta();
            itemVenta.setIdProducto(itemCarrito.getIdProducto());
            itemVenta.setCantidad(itemCarrito.getCantidad());
            itemVenta.setPrecioUnitario(itemCarrito.getPrecioUnitario());
            itemVenta.setDescuentoAplicado(0);
            itemVenta.setVenta(venta);
            venta.getItems().add(itemVenta);
        }

        double total = carrito.getSubtotal() - carrito.getDescuento();
        venta.setTotal(total);
        ventaRepository.save(venta);

        carrito.setEstado(EstadoCarrito.COMPLETADO);
        carritoRepository.save(carrito);

        Factura factura = new Factura();
        factura.setIdVenta(venta.getId());
        factura.setNumeroFactura("FAC-" + UUID.randomUUID());
        factura.setMontoTotal(total);
        factura.setFechaEmision(LocalDateTime.now());
        facturaRepository.save(factura);

        return venta;
    }

    public Venta consultarVenta(Long idVenta) {
        return ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("No existe la venta"));
    }

    public List<Venta> listarHistorial(Long idUsuario) {
        return ventaRepository.findByIdUsuario(idUsuario);
    }

    public Factura emitirFactura(Long idVenta) {
        Optional<Factura> facturaExistente = facturaRepository.findByIdVenta(idVenta);
        if (facturaExistente.isPresent()) {
            return facturaExistente.get();
        }

        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("No existe la venta"));

        if (venta.getEstadoVenta() != EstadoVenta.COMPLETADA) {
            throw new RuntimeException("La venta no está completada");
        }

        Factura factura = new Factura();
        factura.setIdVenta(idVenta);
        factura.setNumeroFactura("FAC-" + UUID.randomUUID());
        factura.setMontoTotal(venta.getTotal());
        factura.setFechaEmision(LocalDateTime.now());
        return facturaRepository.save(factura);
    }

    public String enviarFacturaPorEmail(Long idFactura, String email) {
        Factura factura = facturaRepository.findById(idFactura)
                .orElseThrow(() -> new RuntimeException("No existe la factura"));

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("El email no puede estar vacío");
        }

        return "Factura " + factura.getNumeroFactura() + " enviada a " + email;
    }

    private void validarCarritoActivo(Carrito carrito) {
        if (carrito.getEstado() != EstadoCarrito.ACTIVO) {
            throw new RuntimeException("El carrito no está activo");
        }
    }

    private void recalcularSubtotal(Carrito carrito) {
        double subtotal = carrito.getItems().stream()
                .mapToDouble(item -> item.getPrecioUnitario() * item.getCantidad())
                .sum();
        carrito.setSubtotal(subtotal);

        if ("DESCUENTO10".equals(carrito.getCuponAplicado())) {
            carrito.setDescuento(subtotal * 0.10);
        }
    }

    private ProductoDTO obtenerProducto(Long idProducto) {
        ProductoDTO producto = restTemplate.getForObject(
                URL_CATALOGO + idProducto, ProductoDTO.class);
        if (producto == null) {
            throw new RuntimeException("El producto no existe en el catálogo");
        }
        return producto;
    }

    private ItemInventarioDTO obtenerInventario(Long idProducto) {
        ItemInventarioDTO inventario = restTemplate.getForObject(
                URL_INVENTARIO + idProducto, ItemInventarioDTO.class);
        if (inventario == null) {
            throw new RuntimeException("No se pudo verificar el stock del producto");
        }
        return inventario;
    }

    private void validarUsuario(Long idUsuario) {
        UsuarioDTO usuario = restTemplate.getForObject(
                URL_USUARIOS + idUsuario, UsuarioDTO.class);
        if (usuario == null) {
            throw new RuntimeException("El usuario no existe");
        }
    }
}