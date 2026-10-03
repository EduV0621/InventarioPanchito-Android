package com.panchito.inventario.data.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class EmpleadoAuthDataSource(private val context: Context) {
    suspend fun crearCuenta(correo: String, clave: String): String {
        val auth = FirebaseSecundarioProvider.obtenerAuth(context)
        val resultado = auth.createUserWithEmailAndPassword(correo, clave).esperar()
        return resultado.user?.uid
            ?: throw IllegalStateException("Firebase Authentication no devolvió un usuario nuevo.")
    }

    suspend fun eliminarUltimaCuentaCreada() {
        val auth = FirebaseSecundarioProvider.obtenerAuth(context)
        auth.currentUser?.delete()?.esperar()
    }

    fun cerrarSesionSecundaria() {
        FirebaseSecundarioProvider.obtenerAuth(context).signOut()
    }

    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { resultado -> continuacion.resume(resultado) }
        addOnFailureListener { excepcion -> continuacion.resumeWithException(excepcion) }
    }
}
