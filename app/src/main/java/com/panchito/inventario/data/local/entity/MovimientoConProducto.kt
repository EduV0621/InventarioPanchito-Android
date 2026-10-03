package com.panchito.inventario.data.local.entity

import androidx.room.Embedded

/**
 * Resultado del JOIN movimientos + productos: el movimiento junto con el nombre del producto al que
 * pertenece. Evita que la UI tenga que cruzar dos listas para mostrar el historial.
 */
data class MovimientoConProducto(
    @Embedded val movimiento: MovimientoEntity,
    val productoNombre: String
)
