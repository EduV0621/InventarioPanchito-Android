package com.panchito.inventario.domain.model

/**
 * Modelo de dominio de Categoria de producto (independiente de Room).
 * Los productos referencian una categoria mediante Producto.categoriaId.
 * [estadoSincronizacion] indica si el ultimo cambio ya esta reflejado en el servidor.
 */
data class Categoria(
    val id: Long = 0,
    val nombre: String,
    val activa: Boolean = true,
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO
)
