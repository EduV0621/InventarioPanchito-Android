package com.panchito.inventario.data.auth

import com.panchito.inventario.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface AuthDataSource {
    fun usuarioActual(): Usuario?

    fun observarUsuario(): Flow<Usuario?>

    suspend fun iniciarSesion(correo: String, clave: String): Usuario

    suspend fun registrar(correo: String, clave: String): Usuario

    fun cerrarSesion()
}
