package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmpleadoDto(
    val id: Long = 0,
    @SerializedName("firebase_uid") val firebaseUid: String? = null,
    val nombre: String = "",
    val dni: String = "",
    val telefono: String = "",
    val correo: String = "",

    val rol: String = "EMPLEADO",
    val activo: Boolean = true
)
