package com.panchito.inventario.data.remote.api

import com.panchito.inventario.data.remote.dto.MovimientoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoints REST de movimientos (Fase 3). API propia: PHP + MySQL en XAMPP
 * (htdocs/inventario_api/movimientos.php). Base URL: ver RetrofitClient.
 *
 * Los movimientos son INMUTABLES una vez creados: solo existen "crear" y "listar" (no
 * actualizar/eliminar), a diferencia de ProductoApiService.
 */
interface MovimientoApiService {

    /** Lista completa (inventario compartido) o incremental con ?desde=<updated_at>. */
    @GET("movimientos.php")
    suspend fun obtenerMovimientos(@Query("desde") desde: Long? = null): List<MovimientoDto>

    /** El id (UUID) lo genera Android y viaja en el body, igual que con productos. */
    @POST("movimientos.php")
    suspend fun crearMovimiento(@Body movimiento: MovimientoDto): MovimientoDto
}
