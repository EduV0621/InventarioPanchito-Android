package com.panchito.inventario.data.repository

import com.panchito.inventario.data.auth.AuthDataSource
import com.panchito.inventario.domain.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun usuarioActual(): Usuario?

    fun observarUsuario(): Flow<Usuario?>

    suspend fun iniciarSesion(correo: String, clave: String): Result<Usuario>

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
