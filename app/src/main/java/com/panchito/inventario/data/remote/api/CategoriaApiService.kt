package com.panchito.inventario.data.remote.api

import com.panchito.inventario.data.remote.dto.CategoriaDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CategoriaApiService {
    @GET("categorias.php")
    suspend fun obtenerCategorias(): List<CategoriaDto>

    @POST("categorias.php")
    suspend fun guardarCategoria(@Body categoria: CategoriaDto): CategoriaDto
}
