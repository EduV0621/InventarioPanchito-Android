package com.panchito.inventario.data.remote.api

import com.panchito.inventario.data.remote.dto.EliminarProductoRespuestaDto
import com.panchito.inventario.data.remote.dto.ProductoDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints REST consumidos con Retrofit. API propia: PHP + MySQL en XAMPP
 * (htdocs/inventario_api/productos.php). Base URL: ver RetrofitClient.
 *
 * El backend requiere owner_uid (UID de Firebase Authentication) en casi toda llamada: mismo
 * criterio de separacion de datos por usuario que ya usa Room. El .htaccess del servidor tiene
 * "AcceptPathInfo On", por eso el id va pegado a la ruta (productos.php/{id}).
 */
interface ProductoApiService {

    /** Fase 2 (inventario compartido): ya no requiere owner_uid, devuelve TODO el catalogo. */
    @GET("productos.php")
    suspend fun obtenerProductos(
        @Query("desde") desde: Long? = null
    ): List<ProductoDto>

    @GET("productos.php/{id}")
    suspend fun obtenerProducto(
        @Path("id") id: String
    ): ProductoDto

    /** El id (UUID) lo genera Android y viaja en el body. */
    @POST("productos.php")
    suspend fun crearProducto(@Body producto: ProductoDto): ProductoDto

    @PUT("productos.php/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: String,
        @Body producto: ProductoDto
    ): ProductoDto

    /** Borrado logico (deleted_at) en el servidor. */
    @DELETE("productos.php/{id}")
    suspend fun eliminarProducto(
        @Path("id") id: String
    ): EliminarProductoRespuestaDto
}