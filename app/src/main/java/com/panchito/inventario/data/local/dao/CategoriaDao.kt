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

    /** Usado para bloquear nombres duplicados (comparacion insensible a mayusculas). */
    @Query("SELECT * FROM categorias WHERE LOWER(nombre) = LOWER(:nombre) LIMIT 1")
    suspend fun buscarPorNombre(nombre: String): CategoriaEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(categoria: CategoriaEntity): Long

    @Update
    suspend fun actualizar(categoria: CategoriaEntity)

    // ---- Sincronizacion con la API PHP/MySQL ----

    /** Categorias que todavia no estan reflejadas en el servidor. */
    @Query("SELECT * FROM categorias WHERE estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(): List<CategoriaEntity>

    /**
     * Marca la categoria como enviada, PERO solo si no volvio a cambiar mientras se subia
     * (updatedAt debe seguir siendo el que se envio); asi una edicion hecha en ese lapso no se pierde.
     */
    @Query("UPDATE categorias SET estadoSincronizacion = 'SINCRONIZADO' WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun marcarSincronizado(id: Long, updatedAt: Long): Int

    /** Usada al descargar del servidor: si el id ya existe no se toca. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarIgnorando(categoria: CategoriaEntity): Long

    // TODO Sprint 2: bloquear desactivacion si existen productos activos asociados (regla de negocio).
}
