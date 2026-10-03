package com.panchito.inventario.data.remote.api

import com.panchito.inventario.data.remote.dto.MovimientoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MovimientoApiService {
    @GET("movimientos.php")
    suspend fun obtenerMovimientos(@Query("desde") desde: Long? = null): List<MovimientoDto>

    @POST("movimientos.php")
    suspend fun crearMovimiento(@Body movimiento: MovimientoDto): MovimientoDto
}
