package com.panchito.inventario.data.local.datasource

import com.panchito.inventario.data.local.dao.ProductoDao
import com.panchito.inventario.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos LOCAL de productos: unica clase (junto con su implementacion) que conoce el DAO.
 * El Repository depende de esta interfaz, de modo que en fases futuras pueda combinarse con
 * un RemoteDataSource (Retrofit) sin tocar ViewModels ni UI.
 *
 * Fase 2 (INVENTARIO COMPARTIDO): ya no se filtra por ownerUid; todas las consultas devuelven el
 * catalogo completo del minimarket, compartido por Administrador y Empleados.
 */
interface ProductoLocalDataSource {
    fun observarTodos(): Flow<List<ProductoEntity>>
    fun observarActivos(): Flow<List<ProductoEntity>>
    fun observarPorId(id: String): Flow<ProductoEntity?>
    suspend fun obtenerPorId(id: String): ProductoEntity?
    suspend fun buscarPorCodigo(codigo: String): ProductoEntity?
    suspend fun obtenerUltimoCodigoGenerado(): String?
    suspend fun contarMovimientos(productoId: String): Int
    suspend fun insertar(producto: ProductoEntity)
    suspend fun actualizar(producto: ProductoEntity)
    suspend fun eliminar(producto: ProductoEntity)

    // ---- Sincronizacion ----
    suspend fun obtenerPorIdIncluyendoEliminados(id: String): ProductoEntity?
    suspend fun insertarIgnorando(producto: ProductoEntity): Long
    suspend fun obtenerPendientes(): List<ProductoEntity>
    suspend fun marcarSincronizado(id: String, updatedAt: Long)
}

class RoomProductoLocalDataSource(private val dao: ProductoDao) : ProductoLocalDataSource {
    override fun observarTodos(): Flow<List<ProductoEntity>> = dao.observarTodos()
    override fun observarActivos(): Flow<List<ProductoEntity>> = dao.observarActivos()
    override fun observarPorId(id: String): Flow<ProductoEntity?> = dao.observarPorId(id)
    override suspend fun obtenerPorId(id: String): ProductoEntity? = dao.obtenerPorId(id)
    override suspend fun buscarPorCodigo(codigo: String): ProductoEntity? = dao.buscarPorCodigo(codigo)
    override suspend fun obtenerUltimoCodigoGenerado(): String? = dao.obtenerUltimoCodigoGenerado()
    override suspend fun contarMovimientos(productoId: String): Int = dao.contarMovimientos(productoId)
    override suspend fun insertar(producto: ProductoEntity) = dao.insertar(producto)
    override suspend fun actualizar(producto: ProductoEntity) = dao.actualizar(producto)
    override suspend fun eliminar(producto: ProductoEntity) = dao.eliminar(producto)

    override suspend fun obtenerPorIdIncluyendoEliminados(id: String): ProductoEntity? =
        dao.obtenerPorIdIncluyendoEliminados(id)

    override suspend fun insertarIgnorando(producto: ProductoEntity): Long = dao.insertarIgnorando(producto)

    override suspend fun obtenerPendientes(): List<ProductoEntity> = dao.obtenerPendientes()

    override suspend fun marcarSincronizado(id: String, updatedAt: Long) {
        dao.marcarSincronizado(id, updatedAt)
    }
}
