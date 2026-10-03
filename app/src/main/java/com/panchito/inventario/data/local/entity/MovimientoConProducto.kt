package com.panchito.inventario.data.local.entity

import androidx.room.Embedded

data class MovimientoConProducto(
    @Embedded val movimiento: MovimientoEntity,
    val productoNombre: String
)
