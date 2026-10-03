package com.panchito.inventario.data.remote.datasource

import com.panchito.inventario.data.remote.ConectividadProvider
import com.panchito.inventario.data.remote.RemotoException
import com.panchito.inventario.data.remote.api.ProductoApiService
import com.panchito.inventario.data.remote.dto.EliminarProductoRespuestaDto
import com.panchito.inventario.data.remote.dto.ProductoDto
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * Fuente de datos REMOTA de productos: unica clase que conoce Retrofit. ApiSyncManager depende
 * de esta interfaz, igual que el Repository depende de ProductoLocalDataSource para Room.
 * Todas las operaciones requieren el owner_uid del usuario autenticado.
 */
interface ProductoRemoteDataSource {
    suspend fun obtenerProductos(desde: Long? = null): List<ProductoDto>
    suspend fun obtenerProducto(id: String): ProductoDto
    suspend fun crearProducto(producto: ProductoDto): ProductoDto
    suspend fun actualizarProducto(id: String, producto: ProductoDto): ProductoDto
    suspend fun eliminarProducto(id: String): EliminarProductoRespuestaDto
}

class RetrofitProductoRemoteDataSource(
    private val api: ProductoApiService,
    private val conectividad: ConectividadProvider
) : ProductoRemoteDataSource {

    override suspend fun obtenerProductos(desde: Long?): List<ProductoDto> = ejecutar {
        api.obtenerProductos(desde)
    }

    override suspend fun obtenerProducto(id: String): ProductoDto = ejecutar {
        api.obtenerProducto(id)
    }

    override suspend fun crearProducto(producto: ProductoDto): ProductoDto = ejecutar {
        api.crearProducto(producto)
    }

    override suspend fun actualizarProducto(id: String, producto: ProductoDto): ProductoDto = ejecutar {
        api.actualizarProducto(id, producto)
    }

    override suspend fun eliminarProducto(id: String): EliminarProductoRespuestaDto = ejecutar {
        api.eliminarProducto(id)
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