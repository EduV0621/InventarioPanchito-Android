package com.panchito.inventario.data.local.dao

import androidx.room.*
import com.panchito.inventario.data.local.entity.EmpleadoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpleadoDao {
    @Query("SELECT * FROM empleados ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<EmpleadoEntity>>

    @Query("SELECT * FROM empleados WHERE correo = :correo AND activo = 1 LIMIT 1")
    suspend fun buscarPorCorreoActivo(correo: String): EmpleadoEntity?

    /** Usado para bloquear el registro de un empleado con un DNI ya existente en la cache local. */
    @Query("SELECT * FROM empleados WHERE dni = :dni LIMIT 1")
    suspend fun buscarPorDni(dni: String): EmpleadoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(empleado: EmpleadoEntity): Long

    @Update
    suspend fun actualizar(empleado: EmpleadoEntity)

    // TODO Sprint 2: autenticacion local (PB01); Sprint 4: migracion a Firebase Authentication (PB16).
}
