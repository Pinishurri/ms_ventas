package com.perfulandia.ms_ventas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.perfulandia.ms_ventas.model.ItemVenta;

@Repository
public interface ItemVentaRepository extends JpaRepository<ItemVenta, Long> {
}