package com.panchito.inventario.data.repository

import android.util.Log
import com.panchito.inventario.data.auth.EmpleadoAuthDataSource
import com.panchito.inventario.data.local.datasource.EmpleadoLocalDataSource
import com.panchito.inventario.data.local.entity.EmpleadoEntity
import com.panchito.inventario.data.remote.ConectividadProvider
import com.panchito.inventario.data.remote.datasource.EmpleadoRemoteDataSource
import com.panchito.inventario.data.remote.dto.EmpleadoDto
import com.panchito.inventario.domain.model.Empleado
import com.panchito.inventario.domain.model.Rol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface EmpleadoRepository {
    suspend fun resolverRol(correo: String): Result<Rol>

    suspend fun registrarEmpleado(
        nombre: String,
        dni: String,
        telefono: String,
        correo: String,
        clave: String,
        rol: Rol
    ): Result<Empleado>

    fun observarEmpleados(): Flow<List<Empleado>>

    suspend fun sincronizarUidFirebaseSiFalta(correo: String, uidReal: String)

    suspend fun existeDni(dni: String): Boolean

    fun hayConexion(): Boolean
}

class EmpleadoRepositoryImpl(
    private val remoto: EmpleadoRemoteDataSource,
    private val local: EmpleadoLocalDataSource,
    private val empleadoAuth: EmpleadoAuthDataSource,
    private val conectividad: ConectividadProvider
) : EmpleadoRepository {
    override fun hayConexion(): Boolean = conectividad.hayConexion()

    override suspend fun resolverRol(correo: String): Result<Rol> = try {
        val dto = remoto.obtenerPorCorreo(correo)
        if (dto != null) {
            local.guardar(dto.toEntity())
            Result.success(Rol.valueOf(dto.rol))
        } else {
            Result.failure(IllegalStateException("No existe un empleado activo con ese correo."))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val cache = local.buscarPorCorreoActivo(correo)
        if (cache != null) Result.success(Rol.valueOf(cache.rol)) else Result.failure(e)
    }

    override suspend fun registrarEmpleado(
        nombre: String,
        dni: String,
        telefono: String,
        correo: String,
        clave: String,
        rol: Rol
    ): Result<Empleado> {
        val uid = try {
            empleadoAuth.crearCuenta(correo, clave)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return try {
            val dto = remoto.crearEmpleado(
                EmpleadoDto(
                    firebaseUid = uid,
                    nombre = nombre.trim(),
                    dni = dni.trim(),
                    telefono = telefono.trim(),
                    correo = correo.trim(),
                    rol = rol.name,
                    activo = true
                )
            )
            local.guardar(dto.toEntity())
            empleadoAuth.cerrarSesionSecundaria()
            Result.success(dto.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                empleadoAuth.eliminarUltimaCuentaCreada()
            } catch (errorDeRollback: Exception) {
            }
            empleadoAuth.cerrarSesionSecundaria()
            Result.failure(e)
        }
    }

    override fun observarEmpleados(): Flow<List<Empleado>> =
        local.observarTodos().map { lista -> lista.map { it.toDomain() } }

    override suspend fun existeDni(dni: String): Boolean =
        local.buscarPorDni(dni.trim()) != null

    override suspend fun sincronizarUidFirebaseSiFalta(correo: String, uidReal: String) {
        try {
            val actual = remoto.obtenerPorCorreo(correo) ?: return

            if (!actual.firebaseUid.isNullOrBlank()) return
            val actualizado = remoto.actualizarEmpleado(actual.id, actual.copy(firebaseUid = uidReal))
            local.guardar(actualizado.toEntity())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EmpleadoRepository", "No se pudo sincronizar el firebase_uid de $correo", e)
        }
    }
}

private fun EmpleadoDto.toEntity(): EmpleadoEntity = EmpleadoEntity(
    nombre = nombre,
    dni = dni,
    telefono = telefono,
    correo = correo,
    rol = rol,
    activo = activo
)

private fun EmpleadoDto.toDomain(): Empleado = Empleado(
    nombre = nombre,
    dni = dni,
    telefono = telefono,
    correo = correo,
    rol = Rol.valueOf(rol),
    activo = activo
)

private fun EmpleadoEntity.toDomain(): Empleado = Empleado(
    id = id,
    nombre = nombre,
    dni = dni,
    telefono = telefono,
    correo = correo,
    rol = Rol.valueOf(rol),
    activo = activo
)
