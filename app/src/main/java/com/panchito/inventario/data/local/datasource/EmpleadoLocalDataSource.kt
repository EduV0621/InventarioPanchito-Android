package com.panchito.inventario.data.local.datasource

import com.panchito.inventario.data.local.dao.EmpleadoDao
import com.panchito.inventario.data.local.entity.EmpleadoEntity
import kotlinx.coroutines.flow.Flow

interface EmpleadoLocalDataSource {
    fun observarTodos(): Flow<List<EmpleadoEntity>>
    suspend fun buscarPorCorreoActivo(correo: String): EmpleadoEntity?

    suspend fun buscarPorDni(dni: String): EmpleadoEntity?

    suspend fun guardar(empleado: EmpleadoEntity)
}

class RoomEmpleadoLocalDataSource(private val dao: EmpleadoDao) : EmpleadoLocalDataSource {
    override fun observarTodos(): Flow<List<EmpleadoEntity>> = dao.observarTodos()

    override suspend fun buscarPorCorreoActivo(correo: String): EmpleadoEntity? =
        dao.buscarPorCorreoActivo(correo)

    override suspend fun buscarPorDni(dni: String): EmpleadoEntity? =
        dao.buscarPorDni(dni)

    override suspend fun guardar(empleado: EmpleadoEntity) {
        val existente = dao.buscarPorCorreoActivo(empleado.correo)
        if (existente != null) {
            dao.actualizar(empleado.copy(id = existente.id))
        } else {
            dao.insertar(empleado)
        }
    }
}
