package com.panchito.inventario.data.remote.api

import com.panchito.inventario.data.remote.dto.EmpleadoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface EmpleadoApiService {
    @GET("empleados.php")
    suspend fun obtenerPorCorreo(@Query("correo") correo: String): EmpleadoDto

    @POST("empleados.php")
    suspend fun crearEmpleado(@Body empleado: EmpleadoDto): EmpleadoDto

    @PUT("empleados.php/{id}")
    suspend fun actualizarEmpleado(
        @Path("id") id: Long,
        @Body empleado: EmpleadoDto
    ): EmpleadoDto
}
