package com.panchito.inventario.data.repository

import com.panchito.inventario.data.auth.AuthDataSource
import com.panchito.inventario.domain.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de autenticacion (Fase 4). Los ViewModel solo dependen de esta
 * interfaz y nunca conocen Firebase directamente:
 *
 * UI -> ViewModel -> AuthRepository -> AuthDataSource -> Firebase Authentication
 *
 * Las operaciones de escritura devuelven Result para que el ViewModel muestre los errores
 * sin try/catch (mismo patron que [ProductoRepository]).
 */
interface AuthRepository {

    /** Usuario con sesion activa ahora mismo, segun el estado local de Firebase (sin red). */
    fun usuarioActual(): Usuario?

    /** Emite el usuario actual cada vez que cambia el estado de autenticacion (login/logout). */
    fun observarUsuario(): Flow<Usuario?>

    suspend fun iniciarSesion(correo: String, clave: String): Result<Usuario>

    /**
     * Crea una cuenta en Firebase Authentication. Fase 5: NO se usa desde el Login (no hay registro
     * publico). Queda reservado para la futura gestion de empleados del Administrador.
     *
     * Ojo para esa fase: createUserWithEmailAndPassword deja iniciada la sesion de la cuenta
     * NUEVA en la instancia por defecto de FirebaseAuth, es decir, cerraria la sesion del
     * Administrador. Al implementar "Registrar empleado" habra que crear la cuenta con una
     * instancia secundaria de FirebaseApp o desde un backend (Admin SDK / Cloud Function).
     */
    suspend fun registrar(correo: String, clave: String): Result<Usuario>

    fun cerrarSesion()
}

class AuthRepositoryImpl(
    private val authDataSource: AuthDataSource
) : AuthRepository {

    override fun usuarioActual(): Usuario? = authDataSource.usuarioActual()

    override fun observarUsuario(): Flow<Usuario?> = authDataSource.observarUsuario()

    override suspend fun iniciarSesion(correo: String, clave: String): Result<Usuario> = ejecutar {
        authDataSource.iniciarSesion(correo, clave)
    }

    override suspend fun registrar(correo: String, clave: String): Result<Usuario> = ejecutar {
        authDataSource.registrar(correo, clave)
    }

    override fun cerrarSesion() = authDataSource.cerrarSesion()

    private suspend fun <T> ejecutar(accion: suspend () -> T): Result<T> = try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
