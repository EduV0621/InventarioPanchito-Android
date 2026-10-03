package com.panchito.inventario.data.auth

import com.panchito.inventario.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de datos de autenticacion (Fase 4). Unico contrato que conoce el Repository;
 * la implementacion real con Firebase vive en [FirebaseAuthDataSource].
 */
interface AuthDataSource {

    /** Usuario con sesion activa AHORA MISMO, segun el estado local de Firebase (sin red). */
    fun usuarioActual(): Usuario?

    /** Emite el usuario actual cada vez que cambia el estado de autenticacion (login/logout). */
    fun observarUsuario(): Flow<Usuario?>

    suspend fun iniciarSesion(correo: String, clave: String): Usuario

    suspend fun registrar(correo: String, clave: String): Usuario

    fun cerrarSesion()
}
