package com.panchito.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos",
    indices = [Index(value = ["productoId"]), Index(value = ["ownerUid"])],
    foreignKeys = [
        ForeignKey(entity = ProductoEntity::class, parentColumns = ["id"], childColumns = ["productoId"])
    ]
)
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val productoId: String,
    val ownerUid: String,
    val tipo: String,
    val cantidad: Int,
    val motivo: String,
    val fechaHoraMillis: Long,
    val estadoSincronizacion: String = "PENDIENTE_CREAR",

    val updatedAt: Long = 0L
)
