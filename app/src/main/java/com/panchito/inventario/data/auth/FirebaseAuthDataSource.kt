package com.panchito.inventario.data.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.panchito.inventario.domain.model.Usuario
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementacion de [AuthDataSource] con Firebase Authentication (Email/Password).
 * Es el UNICO archivo de la app que importa clases de com.google.firebase.auth.*;
 * el Repository y el ViewModel solo conocen [Usuario] (modelo de dominio).
 *
 * Firebase Authentication ya se encarga de:
 * - Validar las credenciales contra el servidor (requiere Internet).
 * - Persistir la sesion localmente (SharedPreferences internas del SDK), de forma que
 *   [usuarioActual] siga devolviendo el usuario despues de cerrar y abrir la app, incluso
 *   sin conexion (sesion offline, ver seccion 2 de los requisitos de la Fase 4).
 *
 * No se guarda ninguna contrasena en esta clase ni en ningun otro lugar de la app.
 */
class FirebaseAuthDataSource(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthDataSource {

    override fun usuarioActual(): Usuario? = firebaseAuth.currentUser?.aDominio()

    override fun observarUsuario(): Flow<Usuario?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.aDominio()) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun iniciarSesion(correo: String, clave: String): Usuario =
        firebaseAuth.signInWithEmailAndPassword(correo, clave).esperar().usuarioONulo()

    override suspend fun registrar(correo: String, clave: String): Usuario =
        firebaseAuth.createUserWithEmailAndPassword(correo, clave).esperar().usuarioONulo()

    override fun cerrarSesion() {
        firebaseAuth.signOut()
    }

    private fun AuthResult.usuarioONulo(): Usuario =
        user?.aDominio() ?: throw IllegalStateException("Firebase Authentication no devolvio un usuario")

    private fun FirebaseUser.aDominio() = Usuario(uid = uid, correo = email)

    /**
     * Adapta un Task de Firebase (API basada en callbacks) a una funcion suspendida,
     * sin agregar la dependencia kotlinx-coroutines-play-services solo para esto.
     */
    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { resultado -> continuacion.resume(resultado) }
        addOnFailureListener { excepcion -> continuacion.resumeWithException(excepcion) }
    }
}
