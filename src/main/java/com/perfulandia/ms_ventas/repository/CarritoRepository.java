package com.perfulandia.ms_ventas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.perfulandia.ms_ventas.model.Carrito;
import com.perfulandia.ms_ventas.model.EstadoCarrito;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByIdUsuarioAndEstado(Long idUsuario, EstadoCarrito estado);
}