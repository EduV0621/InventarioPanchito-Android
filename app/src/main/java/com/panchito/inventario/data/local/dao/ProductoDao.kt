package com.panchito.inventario.data.local.dao

import androidx.room.*
import com.panchito.inventario.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE deletedAt IS NULL ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE deletedAt IS NULL AND activo = 1 ORDER BY nombre ASC")
    fun observarActivos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE deletedAt IS NULL AND stock <= stockMinimo AND activo = 1")
    fun observarStockBajo(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun observarPorId(id: String): Flow<ProductoEntity?>

    @Query("SELECT * FROM productos WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun obtenerPorId(id: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorIdIncluyendoEliminados(id: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE codigo = :codigo AND deletedAt IS NULL LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): ProductoEntity?

    @Query("SELECT codigo FROM productos WHERE codigo LIKE 'PRO%' ORDER BY LENGTH(codigo) DESC, codigo DESC LIMIT 1")
    suspend fun obtenerUltimoCodigoGenerado(): String?

    @Query("SELECT COUNT(*) FROM movimientos WHERE productoId = :productoId")
    suspend fun contarMovimientos(productoId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(producto: ProductoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarIgnorando(producto: ProductoEntity): Long

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    @Delete
    suspend fun eliminar(producto: ProductoEntity)

    @Query("SELECT * FROM productos WHERE estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(): List<ProductoEntity>

    @Query(
        """
        UPDATE productos SET estadoSincronizacion = 'SINCRONIZADO'
        WHERE id = :id AND updatedAt = :updatedAt
        """
    )
    suspend fun marcarSincronizado(id: String, updatedAt: Long): Int
}
