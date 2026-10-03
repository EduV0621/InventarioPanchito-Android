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

interface ProductoApiService {
    @GET("productos.php")
    suspend fun obtenerProductos(
        @Query("desde") desde: Long? = null
    ): List<ProductoDto>

    @GET("productos.php/{id}")
    suspend fun obtenerProducto(
        @Path("id") id: String
    ): ProductoDto

    @POST("productos.php")
    suspend fun crearProducto(@Body producto: ProductoDto): ProductoDto

    @PUT("productos.php/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: String,
        @Body producto: ProductoDto
    ): ProductoDto

    @DELETE("productos.php/{id}")
    suspend fun eliminarProducto(
        @Path("id") id: String
    ): EliminarProductoRespuestaDto
}
