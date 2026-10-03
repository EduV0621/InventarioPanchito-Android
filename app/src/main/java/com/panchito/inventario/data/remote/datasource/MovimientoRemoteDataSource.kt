package com.panchito.inventario.data.remote.datasource

import com.panchito.inventario.data.remote.ConectividadProvider
import com.panchito.inventario.data.remote.RemotoException
import com.panchito.inventario.data.remote.api.MovimientoApiService
import com.panchito.inventario.data.remote.dto.MovimientoDto
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

interface MovimientoRemoteDataSource {
    suspend fun obtenerMovimientos(desde: Long? = null): List<MovimientoDto>
    suspend fun crearMovimiento(movimiento: MovimientoDto): MovimientoDto
}

class RetrofitMovimientoRemoteDataSource(
    private val api: MovimientoApiService,
    private val conectividad: ConectividadProvider
) : MovimientoRemoteDataSource {
    override suspend fun obtenerMovimientos(desde: Long?): List<MovimientoDto> = ejecutar {
        api.obtenerMovimientos(desde)
    }

    override suspend fun crearMovimiento(movimiento: MovimientoDto): MovimientoDto = ejecutar {
        api.crearMovimiento(movimiento)
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

    private fun extraerMensajeDeError(e: HttpException): String? = try {
        val cuerpo = e.response()?.errorBody()?.string()
        if (cuerpo.isNullOrBlank()) null else JSONObject(cuerpo).optString("error").takeIf { it.isNotBlank() }
    } catch (ex: Exception) {
        null
    }
}
