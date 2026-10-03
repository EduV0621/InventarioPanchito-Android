package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductoDto(
    val id: String = "",
    @SerializedName("owner_uid") val ownerUid: String = "",
    val codigo: String = "",
    val nombre: String = "",
    @SerializedName("categoria_id") val categoriaId: Long = 0L,
    val precio: Double = 0.0,
    val stock: Int = 0,
    @SerializedName("stock_minimo") val stockMinimo: Int = 0,

    @SerializedName("fecha_vencimiento") val fechaVencimiento: String? = null,
    val activo: Boolean = true,
    @SerializedName("updated_at") val updatedAt: Long = 0L,
    @SerializedName("deleted_at") val deletedAt: Long? = null
)

data class EliminarProductoRespuestaDto(
    val id: String = "",
    val eliminado: Boolean = false
)
