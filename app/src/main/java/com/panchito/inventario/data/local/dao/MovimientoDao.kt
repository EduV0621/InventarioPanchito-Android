package com.panchito.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.panchito.inventario.data.local.entity.MovimientoConProducto
import com.panchito.inventario.data.local.entity.MovimientoEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MovimientoDao {
    @Query(
        """
        SELECT m.*, p.nombre AS productoNombre
        FROM movimientos m
        INNER JOIN productos p ON p.id = m.productoId
        ORDER BY m.fechaHoraMillis DESC
        """
    )
    abstract fun observarConProducto(): Flow<List<MovimientoConProducto>>

    @Query("SELECT * FROM movimientos WHERE estadoSincronizacion != 'SINCRONIZADO'")
    abstract suspend fun obtenerPendientesDeSincronizar(): List<MovimientoEntity>

    @Query("SELECT * FROM movimientos WHERE id = :id LIMIT 1")
    abstract suspend fun obtenerPorId(id: String): MovimientoEntity?

    @Query("UPDATE movimientos SET estadoSincronizacion = 'SINCRONIZADO' WHERE id = :id")
    abstract suspend fun marcarSincronizado(id: String): Int

    @Insert
    abstract suspend fun insertar(movimiento: MovimientoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarIgnorando(movimiento: MovimientoEntity): Long

    @Query(
        """
        UPDATE productos
        SET stock = :nuevoStock,
            fechaVencimientoMillis = CASE
                WHEN :fechaVencimientoMillis IS NOT NULL THEN :fechaVencimientoMillis
                ELSE fechaVencimientoMillis
            END,
            updatedAt = :updatedAt,
            estadoSincronizacion = CASE
                WHEN estadoSincronizacion = 'PENDIENTE_CREAR' THEN 'PENDIENTE_CREAR'
                ELSE 'PENDIENTE_ACTUALIZAR'
            END
        WHERE id = :productoId AND deletedAt IS NULL
        """
    )
    abstract suspend fun actualizarStock(
        productoId: String,
        nuevoStock: Int,
        fechaVencimientoMillis: Long?,
        updatedAt: Long
    ): Int

    @Transaction
    open suspend fun registrarYActualizarStock(
        movimiento: MovimientoEntity,
        nuevoStock: Int,
        fechaVencimientoMillis: Long?
    ): Boolean {
        val filas = actualizarStock(movimiento.productoId, nuevoStock, fechaVencimientoMillis, movimiento.updatedAt)
        if (filas == 0) return false
        insertar(movimiento)
        return true
    }
}
