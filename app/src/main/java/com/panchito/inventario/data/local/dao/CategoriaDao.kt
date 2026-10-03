package com.panchito.inventario.data.local.dao

import androidx.room.*
import com.panchito.inventario.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun observarTodas(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): CategoriaEntity?

    @Query("SELECT * FROM categorias WHERE LOWER(nombre) = LOWER(:nombre) LIMIT 1")
    suspend fun buscarPorNombre(nombre: String): CategoriaEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(categoria: CategoriaEntity): Long

    @Update
    suspend fun actualizar(categoria: CategoriaEntity)

    @Query("SELECT * FROM categorias WHERE estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(): List<CategoriaEntity>

    @Query("UPDATE categorias SET estadoSincronizacion = 'SINCRONIZADO' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun marcarSincronizado(id: Long, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarIgnorando(categoria: CategoriaEntity): Long
}
