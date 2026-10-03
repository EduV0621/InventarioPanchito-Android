package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de Categoria para la capa REMOTA: el JSON exacto que entrega/espera
 * htdocs/inventario_api/categorias.php. El id es el mismo que en Room.
 */
data class CategoriaDto(
    val id: Long = 0L,
    val nombre: String = "",
    /** true = activa (1 en MySQL), false = inactiva (0 en MySQL). */
    val activa: Boolean = true,
    @SerializedName("updated_at") val updatedAt: Long = 0L
)
