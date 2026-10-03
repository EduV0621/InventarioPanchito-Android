package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de Producto para la capa REMOTA: representa el JSON exacto que entrega/espera
 * htdocs/inventario_api/productos.php (funciones filaAJson / datosParaSql).
 *
 * A diferencia de la API de prueba anterior (DummyJSON), este backend guarda el MISMO modelo
 * que Room: mismos campos, mismo id (UUID) y mismo owner_uid de Firebase Authentication. La
 * unica conversion real es fecha_vencimiento, que MySQL maneja como DATE ("YYYY-MM-DD") en vez
 * de millis (ver formatearFechaApi/parsearFechaApi en data/mapper/Mappers.kt).
 */
data class ProductoDto(
    val id: String = "",
    @SerializedName("owner_uid") val ownerUid: String = "",
    val codigo: String = "",
    val nombre: String = "",
    @SerializedName("categoria_id") val categoriaId: Long = 0L,
    val precio: Double = 0.0,
    val stock: Int = 0,
    @SerializedName("stock_minimo") val stockMinimo: Int = 0,
    /** "YYYY-MM-DD" o null. */
    @SerializedName("fecha_vencimiento") val fechaVencimiento: String? = null,
    val activo: Boolean = true,
    @SerializedName("updated_at") val updatedAt: Long = 0L,
    @SerializedName("deleted_at") val deletedAt: Long? = null
)

/** Respuesta de DELETE /productos.php/{id}: {"id": "...", "eliminado": true}. */
data class EliminarProductoRespuestaDto(
    val id: String = "",
    val eliminado: Boolean = false
)