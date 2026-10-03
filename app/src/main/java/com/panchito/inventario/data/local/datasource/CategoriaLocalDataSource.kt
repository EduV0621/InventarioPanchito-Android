package com.panchito.inventario.data.local.datasource

import com.panchito.inventario.data.local.dao.CategoriaDao
import com.panchito.inventario.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

interface CategoriaLocalDataSource {
    fun observarTodas(): Flow<List<CategoriaEntity>>
    suspend fun buscarPorId(id: Long): CategoriaEntity?
    suspend fun buscarPorNombre(nombre: String): CategoriaEntity?
    suspend fun insertar(categoria: CategoriaEntity): Long
    suspend fun actualizar(categoria: CategoriaEntity)

    suspend fun obtenerPendientes(): List<CategoriaEntity>
    suspend fun marcarSincronizado(id: Long, updatedAt: Long)
    suspend fun insertarIgnorando(categoria: CategoriaEntity): Long
}

class RoomCategoriaLocalDataSource(private val dao: CategoriaDao) : CategoriaLocalDataSource {
    override fun observarTodas(): Flow<List<CategoriaEntity>> = dao.observarTodas()
    override suspend fun buscarPorId(id: Long): CategoriaEntity? = dao.buscarPorId(id)
    override suspend fun buscarPorNombre(nombre: String): CategoriaEntity? = dao.buscarPorNombre(nombre)
    override suspend fun insertar(categoria: CategoriaEntity): Long = dao.insertar(categoria)
    override suspend fun actualizar(categoria: CategoriaEntity) = dao.actualizar(categoria)

    override suspend fun obtenerPendientes(): List<CategoriaEntity> = dao.obtenerPendientes()
    override suspend fun marcarSincronizado(id: Long, updatedAt: Long) {
        dao.marcarSincronizado(id, updatedAt)
    }
    override suspend fun insertarIgnorando(categoria: CategoriaEntity): Long = dao.insertarIgnorando(categoria)
}
