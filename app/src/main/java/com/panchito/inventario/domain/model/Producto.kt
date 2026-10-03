package com.panchito.inventario.domain.model

import java.util.Date

data class Producto(
    val id: String = "",
    val codigo: String,
    val nombre: String,
    val categoriaId: Long,
    val precio: Double,
    val stock: Int,
    val stockMinimo: Int,
    val fechaVencimiento: Date? = null,
    val activo: Boolean = true,

    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,

    val updatedAt: Long = System.currentTimeMillis(),

    val deletedAt: Long? = null
) {
    val stockBajo: Boolean get() = stock in 1..stockMinimo

    val agotado: Boolean get() = stock == 0
}
