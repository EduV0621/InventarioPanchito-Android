package com.panchito.inventario.data.local.datasource

import com.panchito.inventario.data.local.dao.EmpleadoDao
import com.panchito.inventario.data.local.entity.EmpleadoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos LOCAL de empleados (Fase 1): unica clase que conoce Room para este modulo,
 * mismo patron que [RoomProductoLocalDataSource]/[RoomMovimientoLocalDataSource].
 *
 * Sirve como cache offline del rol resuelto contra el backend (ver EmpleadoRepositoryImpl):
 * si no hay conexion al iniciar sesion, se usa la ultima copia guardada aqui.
 */
interface EmpleadoLocalDataSource {
    fun observarTodos(): Flow<List<EmpleadoEntity>>
    suspend fun buscarPorCorreoActivo(correo: String): EmpleadoEntity?

    /** Usado para validar, antes de registrar, que el DNI no pertenezca ya a otro empleado. */
    suspend fun buscarPorDni(dni: String): EmpleadoEntity?

    /** Inserta si es nuevo (por correo) o actualiza sus datos si ya existia. */
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
