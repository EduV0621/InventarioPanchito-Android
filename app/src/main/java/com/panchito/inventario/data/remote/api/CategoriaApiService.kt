package com.panchito.inventario.data.remote.api

import com.panchito.inventario.data.remote.dto.CategoriaDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Endpoints REST de categorias. API propia: PHP + MySQL en XAMPP
 * (htdocs/inventario_api/categorias.php). Base URL: ver RetrofitClient.
 */
interface CategoriaApiService {

    /** Todas las categorias (incluidas las inactivas). */
    @GET("categorias.php")
    suspend fun obtenerCategorias(): List<CategoriaDto>

    /**
     * Crea o actualiza (segun exista ya ese id en MySQL). Es idempotente: reintentar el mismo
     * envio no duplica nada, por eso la sincronizacion usa este unico endpoint para
     * altas, ediciones y cambios de estado activa/inactiva.
     */
    @POST("categorias.php")
    suspend fun guardarCategoria(@Body categoria: CategoriaDto): CategoriaDto
}
