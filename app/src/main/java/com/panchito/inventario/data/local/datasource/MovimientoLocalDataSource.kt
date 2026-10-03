package com.panchito.inventario.data.local.datasource

import com.panchito.inventario.data.local.dao.MovimientoDao
import com.panchito.inventario.data.local.entity.MovimientoConProducto
import com.panchito.inventario.data.local.entity.MovimientoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos LOCAL de movimientos: unica clase (junto con su implementacion) que conoce el DAO,
 * siguiendo el mismo patron que [ProductoLocalDataSource].
 *
 * Fase 2/3 (INVENTARIO Y HISTORIAL COMPARTIDOS): ya no se filtra por ownerUid.
 */
interface MovimientoLocalDataSource {
    fun observarConProducto(): Flow<List<MovimientoConProducto>>
    suspend fun registrarYActualizarStock(
        movimiento: MovimientoEntity,
        nuevoStock: Int,
        fechaVencimientoMillis: Long?
    ): Boolean
    suspend fun insertar(movimiento: MovimientoEntity)

    // ---- Sincronizacion (Fase 3) ----
    suspend fun obtenerPendientes(): List<MovimientoEntity>
    suspend fun obtenerPorId(id: String): MovimientoEntity?
    suspend fun marcarSincronizado(id: String)
    suspend fun insertarIgnorando(movimiento: MovimientoEntity): Long
}

class RoomMovimientoLocalDataSource(private val dao: MovimientoDao) : MovimientoLocalDataSource {
    override fun observarConProducto(): Flow<List<MovimientoConProducto>> =
        dao.observarConProducto()

    override suspend fun registrarYActualizarStock(
        movimiento: MovimientoEntity,
        nuevoStock: Int,
        fechaVencimientoMillis: Long?
    ): Boolean = dao.registrarYActualizarStock(movimiento, nuevoStock, fechaVencimientoMillis)

    override suspend fun insertar(movimiento: MovimientoEntity) = dao.insertar(movimiento)

    override suspend fun obtenerPendientes(): List<MovimientoEntity> =
        dao.obtenerPendientesDeSincronizar()

    override suspend fun obtenerPorId(id: String): MovimientoEntity? =
        dao.obtenerPorId(id)

    override suspend fun marcarSincronizado(id: String) {
        dao.marcarSincronizado(id)
    }

    override suspend fun insertarIgnorando(movimiento: MovimientoEntity): Long =
        dao.insertarIgnorando(movimiento)
}
