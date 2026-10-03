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

    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { resultado -> continuacion.resume(resultado) }
        addOnFailureListener { excepcion -> continuacion.resumeWithException(excepcion) }
    }
}
