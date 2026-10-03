package com.panchito.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room de Empleado.
 * Nota: las contrasenas NO se almacenan aqui; seran gestionadas exclusivamente
 * por Firebase Authentication a partir del Sprint 4 (ver informe, Reglas de negocio).
 */
@Entity(
    tableName = "empleados",
    indices = [Index(value = ["dni"], unique = true), Index(value = ["correo"], unique = true)]
)
data class EmpleadoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val dni: String,
    val telefono: String,
    val correo: String,
    val rol: String, // "ADMINISTRADOR" | "EMPLEADO"
    val activo: Boolean = true
)
