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

/**
 * Repositorio de empleados/roles (Fase 1).
 *
 * UI -> ViewModel -> EmpleadoRepository -> EmpleadoRemoteDataSource (backend PHP)
 *                                        -> EmpleadoLocalDataSource (cache Room, uso offline)
 *                                        -> EmpleadoAuthDataSource (Firebase, solo para registrar)
 */
interface EmpleadoRepository {

    /**
     * Resuelve el rol de un correo justo despues del login: consulta el backend (fuente de
     * verdad) y cachea el resultado en Room. Si no hay conexion, usa la ultima copia local
     * conocida para ese correo (permite iniciar sesion sin Internet, igual que ya hace Firebase
     * Authentication con la sesion local).
     */
    suspend fun resolverRol(correo: String): Result<Rol>

    /**
     * El Administrador registra un empleado nuevo:
     * 1) crea su cuenta en Firebase Authentication (instancia secundaria, no cierra la sesion
     *    del Administrador que esta logueado en la instancia por defecto);
     * 2) guarda sus datos y rol en el backend (MySQL vía empleados.php);
     * 3) cachea el registro en Room.
     * Si el paso 2 falla, se revierte la cuenta de Firebase creada en el paso 1 (rollback).
     */
    suspend fun registrarEmpleado(
        nombre: String,
        dni: String,
        telefono: String,
        correo: String,
        clave: String,
        rol: Rol
    ): Result<Empleado>

    /** Lista de empleados cacheada localmente (Room), para uso futuro de la pantalla de gestion. */
    fun observarEmpleados(): Flow<List<Empleado>>

    /**
     * Fase 1 (fix del UID del admin): el administrador inicial se siembra por SQL directamente
     * en MySQL, sin pasar por esta API, asi que su fila queda con firebase_uid NULO. Esta funcion
     * se llama despues de cada login exitoso: si el empleado de ese correo todavia no tiene
     * firebase_uid guardado, lo completa con el [uidReal] de la sesion de Firebase recien
     * iniciada (PUT empleados.php/{id}). Si el empleado ya tenia firebase_uid, o el correo no
     * existe, o falla por cualquier motivo (p. ej. sin conexion), no hace nada: es una operacion
     * de "mejor esfuerzo" que NUNCA debe bloquear ni hacer fallar el login, y que se reintenta
     * sola en el siguiente inicio de sesion.
     */
    suspend fun sincronizarUidFirebaseSiFalta(correo: String, uidReal: String)

    /**
     * true si ya existe un empleado (activo o inactivo) con ese DNI en la cache local.
     * Se consulta ANTES de crear la cuenta en Firebase Authentication, para no dejar cuentas
     * huerfanas si el registro se va a rechazar por DNI duplicado.
     * Nota: la verificacion es contra la copia local (Room) de este dispositivo; el backend
     * (empleados.php) deberia, ademas, aplicar su propia restriccion de unicidad de DNI.
     */
    suspend fun existeDni(dni: String): Boolean

    /**
     * true si el dispositivo tiene una red con Internet. Registrar un empleado necesita Internet
     * (Firebase Authentication + backend), asi que el ViewModel lo consulta ANTES de empezar, para
     * avisar de inmediato en vez de dejar que Firebase falle con un error tecnico.
     */
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
        // Backend no disponible (p. ej. sin Internet): se recurre a la ultima copia local.
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
            // El backend no guardo el registro: revertir la cuenta de Firebase para no dejarla huerfana.
            try {
                empleadoAuth.eliminarUltimaCuentaCreada()
            } catch (errorDeRollback: Exception) {
                // Best-effort: se prioriza informar el error original de la operacion.
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
            // Ya tiene firebase_uid guardado (caso normal, empleados creados desde la app): nada
            // que hacer. Solo el sembrado inicial por SQL puede llegar con esto nulo/vacio.
            if (!actual.firebaseUid.isNullOrBlank()) return
            val actualizado = remoto.actualizarEmpleado(actual.id, actual.copy(firebaseUid = uidReal))
            local.guardar(actualizado.toEntity())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best-effort: si falla (sin conexion, backend caido, etc.) se reintenta solo en el
            // siguiente login; nunca debe interrumpir el flujo de inicio de sesion del usuario.
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
