package com.panchito.inventario.data.remote.datasource

import com.panchito.inventario.data.remote.ConectividadProvider
import com.panchito.inventario.data.remote.RemotoException
import com.panchito.inventario.data.remote.api.CategoriaApiService
import com.panchito.inventario.data.remote.dto.CategoriaDto
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente de datos REMOTA de categorias: unica clase que conoce Retrofit para este modulo, con el
 * mismo patron que [ProductoRemoteDataSource].
 */
interface CategoriaRemoteDataSource {
    suspend fun obtenerCategorias(): List<CategoriaDto>
    suspend fun guardarCategoria(categoria: CategoriaDto): CategoriaDto
}

class RetrofitCategoriaRemoteDataSource(
    private val api: CategoriaApiService,
    private val conectividad: ConectividadProvider
) : CategoriaRemoteDataSource {

    override suspend fun obtenerCategorias(): List<CategoriaDto> = ejecutar {
        api.obtenerCategorias()
    }

    override suspend fun guardarCategoria(categoria: CategoriaDto): CategoriaDto = ejecutar {
        api.guardarCategoria(categoria)
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
