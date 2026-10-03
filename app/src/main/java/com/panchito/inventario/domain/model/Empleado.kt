package com.panchito.inventario.domain.model

data class Empleado(
    val id: Long = 0,
    val nombre: String,
    val dni: String,
    val telefono: String,
    val correo: String,
    val rol: Rol,
    val activo: Boolean = true
)
