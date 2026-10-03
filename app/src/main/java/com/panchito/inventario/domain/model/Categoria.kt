package com.panchito.inventario.domain.model

data class Categoria(
    val id: Long = 0,
    val nombre: String,
    val activa: Boolean = true,
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO
)
