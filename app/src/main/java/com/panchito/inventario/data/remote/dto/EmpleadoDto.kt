package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de Empleado para la capa REMOTA (Fase 1): representa el JSON exacto que entrega/espera
 * htdocs/inventario_api/empleados.php.
 *
 * firebase_uid puede venir null (p. ej. el administrador inicial sembrado por SQL, cuya cuenta de
 * Firebase se creo por fuera de esta API).
 */
data class EmpleadoDto(
    val id: Long = 0,
    @SerializedName("firebase_uid") val firebaseUid: String? = null,
    val nombre: String = "",
    val dni: String = "",
    val telefono: String = "",
    val correo: String = "",
    /** "ADMINISTRADOR" | "EMPLEADO" */
    val rol: String = "EMPLEADO",
    val activo: Boolean = true
)
