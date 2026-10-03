package com.panchito.inventario.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de Movimiento para la capa REMOTA (Fase 3): representa el JSON exacto que entrega/espera
 * htdocs/inventario_api/movimientos.php (funciones filaAJson / datosParaSql).
 *
 * Los movimientos son INMUTABLES: solo se crean (POST) y se listan (GET); no hay
 * actualizar/eliminar, a diferencia de ProductoDto.
 */
data class MovimientoDto(
    val id: String = "",
    @SerializedName("producto_id") val productoId: String = "",
    /** UID de Firebase Authentication de quien registro el movimiento (solo trazabilidad). */
    @SerializedName("creado_por_uid") val creadoPorUid: String = "",
    /** "ENTRADA" | "SALIDA" */
    val tipo: String = "ENTRADA",
    val cantidad: Int = 0,
    val motivo: String = "",
    /** epoch millis. */
    @SerializedName("fecha_hora") val fechaHora: Long = 0L,
    @SerializedName("updated_at") val updatedAt: Long = 0L
)
