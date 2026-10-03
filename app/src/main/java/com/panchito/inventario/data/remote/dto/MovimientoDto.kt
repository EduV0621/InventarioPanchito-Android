package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MovimientoDto(
    val id: String = "",
    @SerializedName("producto_id") val productoId: String = "",

    @SerializedName("creado_por_uid") val creadoPorUid: String = "",

    val tipo: String = "ENTRADA",
    val cantidad: Int = 0,
    val motivo: String = "",

    @SerializedName("fecha_hora") val fechaHora: Long = 0L,
    @SerializedName("updated_at") val updatedAt: Long = 0L
)
