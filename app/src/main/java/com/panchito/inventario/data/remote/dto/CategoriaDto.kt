package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CategoriaDto(
    val id: Long = 0L,
    val nombre: String = "",

    val activa: Boolean = true,
    @SerializedName("updated_at") val updatedAt: Long = 0L
)
