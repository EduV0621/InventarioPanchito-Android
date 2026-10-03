package com.panchito.inventario.domain.model

/**
 * Modelo de dominio de Empleado/Usuario del sistema.
 * Un empleado con estado inactivo no puede iniciar sesion (ver Reglas de negocio del informe).
 */
data class Empleado(
    val id: Long = 0,
    val nombre: String,
    val dni: String,
    val telefono: String,
    val correo: String,
    val rol: Rol,
    val activo: Boolean = true
)
