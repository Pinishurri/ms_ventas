package com.perfulandia.ms_ventas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemInventarioDTO {
    private Long idProducto;
    private int cantidadDisponible;
}