package com.panchito.inventario.data.remote.api

import com.panchito.inventario.data.remote.dto.EmpleadoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints REST de empleados/roles (Fase 1). API propia: PHP + MySQL en XAMPP
 * (htdocs/inventario_api/empleados.php). Base URL: ver RetrofitClient.
 */
interface EmpleadoApiService {

    /**
     * Usado tras el login para resolver el rol del usuario autenticado.
     * El backend responde 404 si no existe un empleado activo con ese correo.
     */
    @GET("empleados.php")
    suspend fun obtenerPorCorreo(@Query("correo") correo: String): EmpleadoDto

    /** El Administrador registra un empleado nuevo; el id lo genera MySQL (AUTO_INCREMENT). */
    @POST("empleados.php")
    suspend fun crearEmpleado(@Body empleado: EmpleadoDto): EmpleadoDto

    /**
     * Fase 1 (fix del UID del admin): actualiza un empleado existente. Se usa justo despues del
     * login para completar el firebase_uid del administrador inicial (sembrado por SQL sin
     * firebase_uid, porque su cuenta de Firebase se creo por fuera de esta API).
     */
    @PUT("empleados.php/{id}")
    suspend fun actualizarEmpleado(
        @Path("id") id: Long,
        @Body empleado: EmpleadoDto
    ): EmpleadoDto
}
