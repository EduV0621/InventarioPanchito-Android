package com.panchito.inventario.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.panchito.inventario.data.local.entity.MovimientoConProducto
import com.panchito.inventario.data.local.entity.MovimientoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Se declara como clase abstracta (en vez de interfaz) porque incluye un metodo @Transaction con
 * cuerpo, que es la forma que recomienda Room para agrupar varias escrituras en una sola transaccion.
 *
 * Fase 2/3 (INVENTARIO Y HISTORIAL COMPARTIDOS): ya no se filtra por ownerUid. El historial de
 * movimientos, igual que el catalogo de productos, es el mismo para el Administrador y para todos
 * los Empleados. ownerUid se conserva en la fila solo como trazabilidad de quien hizo el
 * movimiento. El historial se obtiene con un JOIN contra productos para mostrar el nombre del producto.
 */
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

    /** Movimientos de TODO el inventario compartido que todavia no se subieron al servidor. */
    @Query("SELECT * FROM movimientos WHERE estadoSincronizacion != 'SINCRONIZADO'")
    abstract suspend fun obtenerPendientesDeSincronizar(): List<MovimientoEntity>

    @Query("SELECT * FROM movimientos WHERE id = :id LIMIT 1")
    abstract suspend fun obtenerPorId(id: String): MovimientoEntity?

    @Query("UPDATE movimientos SET estadoSincronizacion = 'SINCRONIZADO' WHERE id = :id")
    abstract suspend fun marcarSincronizado(id: String): Int

    @Insert
    abstract suspend fun insertar(movimiento: MovimientoEntity)

    /** Usada al descargar movimientos de otros dispositivos: si ya existe (mismo id) no se toca. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarIgnorando(movimiento: MovimientoEntity): Long

    /**
     * Fase 2: el producto tambien queda pendiente de subir cuando se mueve su stock. Si todavia
     * estaba en PENDIENTE_CREAR se conserva ese estado (el registro remoto aun no existe, no
     * puede pasar a "actualizar"). Las lapidas (deletedAt) quedan fuera. Ya no filtra por
     * ownerUid: el stock es del inventario compartido, no de un usuario en particular.
     */
    /**
     * [fechaVencimientoMillis] es OPCIONAL (puede venir null desde el formulario de movimientos,
     * por ejemplo en productos de limpieza que no vencen): si es null se conserva la fecha que ya
     * tenia el producto; si viene con valor, la reemplaza. La fecha de vencimiento sigue siendo un
     * dato del PRODUCTO (columna de la tabla productos), no una columna nueva en movimientos.
     */
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

    /**
     * Actualiza el stock (y, si se indico, la fecha de vencimiento) del producto y registra el
     * movimiento en una MISMA transaccion: si algo falla no queda el producto actualizado sin su
     * movimiento, ni al reves.
     * Devuelve false si el producto no existe (no se toco nada).
     */
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
