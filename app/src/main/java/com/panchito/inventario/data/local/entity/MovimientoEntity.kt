package com.panchito.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room de Movimiento (entradas/salidas de inventario).
 *
 * Fase 5.2:
 * - [id] pasa a ser un UUID (String), igual que el de Producto.
 * - [productoId] mantiene la clave foranea hacia productos.id (el ID ESTABLE del producto).
 * - [ownerUid] (UID de Firebase Authentication) reemplaza al antiguo usuarioId, que apuntaba a la
 *   tabla empleados: la gestion de empleados todavia no existe y el duenio real de los datos es el
 *   usuario autenticado, igual que en productos.
 * - [estadoSincronizacion] se conserva PENDIENTE; la sincronizacion con la nube no se implementa aqui.
 */
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
    val tipo: String, // "ENTRADA" | "SALIDA"
    val cantidad: Int,
    val motivo: String,
    val fechaHoraMillis: Long,
    val estadoSincronizacion: String = "PENDIENTE_CREAR",
    /** Fase 6: ultima modificacion local (epoch millis), usada para resolver conflictos. */
    val updatedAt: Long = 0L
)
