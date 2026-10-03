package com.panchito.inventario.data.remote.datasource

import com.panchito.inventario.data.remote.ConectividadProvider
import com.panchito.inventario.data.remote.RemotoException
import com.panchito.inventario.data.remote.api.EmpleadoApiService
import com.panchito.inventario.data.remote.dto.EmpleadoDto
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente de datos REMOTA de empleados/roles (Fase 1): unica clase que conoce Retrofit para este
 * modulo, con el mismo patron que [ProductoRemoteDataSource] usa para productos.
 */
interface EmpleadoRemoteDataSource {
    /** null si no existe un empleado ACTIVO con ese correo (404 del backend), en vez de excepcion. */
    suspend fun obtenerPorCorreo(correo: String): EmpleadoDto?

    suspend fun crearEmpleado(empleado: EmpleadoDto): EmpleadoDto

    /** Fase 1 (fix del UID del admin): PUT empleados.php/{id}. */
    suspend fun actualizarEmpleado(id: Long, empleado: EmpleadoDto): EmpleadoDto
}

class RetrofitEmpleadoRemoteDataSource(
    private val api: EmpleadoApiService,
    private val conectividad: ConectividadProvider
) : EmpleadoRemoteDataSource {

    override suspend fun obtenerPorCorreo(correo: String): EmpleadoDto? {
        if (!conectividad.hayConexion()) throw RemotoException.SinConexion()
        return try {
            api.obtenerPorCorreo(correo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // 404 = "no existe ese correo como empleado activo", no es un error de conexion/servidor.
            if (e.code() == 404) null else throw RemotoException.ErrorServidor(e.code(), extraerMensajeDeError(e))
        } catch (e: IOException) {
            throw RemotoException.SinConexion()
        } catch (e: Exception) {
            throw RemotoException.Desconocido(e.message ?: "")
        }
    }

    override suspend fun crearEmpleado(empleado: EmpleadoDto): EmpleadoDto = ejecutar {
        api.crearEmpleado(empleado)
    }

    override suspend fun actualizarEmpleado(id: Long, empleado: EmpleadoDto): EmpleadoDto = ejecutar {
        api.actualizarEmpleado(id, empleado)
    }

    private suspend inline fun <T> ejecutar(crossinline accion: suspend () -> T): T {
        if (!conectividad.hayConexion()) throw RemotoException.SinConexion()
        return try {
            accion()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw RemotoException.SinConexion()
        } catch (e: HttpException) {
            throw RemotoException.ErrorServidor(e.code(), extraerMensajeDeError(e))
        } catch (e: Exception) {
            throw RemotoException.Desconocido(e.message ?: "")
        }
    }

    /** Lee el campo {"error": "..."} que devuelve Response::error() en el backend PHP, si existe. */
    private fun extraerMensajeDeError(e: HttpException): String? = try {
        val cuerpo = e.response()?.errorBody()?.string()
        if (cuerpo.isNullOrBlank()) null else JSONObject(cuerpo).optString("error").takeIf { it.isNotBlank() }
    } catch (ex: Exception) {
        null
    }
}
