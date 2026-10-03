package com.panchito.inventario.data.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Crea cuentas de Firebase Authentication para EMPLEADOS NUEVOS (Fase 1, "Registro de
 * empleado"), usando siempre la instancia SECUNDARIA de [FirebaseSecundarioProvider]: la sesion
 * del Administrador (instancia por defecto, ver [FirebaseAuthDataSource]) nunca se toca.
 */
class EmpleadoAuthDataSource(private val context: Context) {

    /**
     * Crea la cuenta en Firebase Authentication y devuelve su uid.
     *
     * Mientras no se llame a [cerrarSesionSecundaria] o [eliminarUltimaCuentaCreada], la cuenta
     * nueva queda "logueada" en la instancia SECUNDARIA (nunca en la del Administrador), lo que
     * permite revertirla si el registro no se completa en el backend.
     */
    suspend fun crearCuenta(correo: String, clave: String): String {
        val auth = FirebaseSecundarioProvider.obtenerAuth(context)
        val resultado = auth.createUserWithEmailAndPassword(correo, clave).esperar()
        return resultado.user?.uid
            ?: throw IllegalStateException("Firebase Authentication no devolvió un usuario nuevo.")
    }

    /**
     * Revierte [crearCuenta] cuando el registro no se pudo completar en el backend: borra la
     * cuenta recien creada para no dejar cuentas de Firebase "huerfanas" (sin fila en MySQL).
     */
    suspend fun eliminarUltimaCuentaCreada() {
        val auth = FirebaseSecundarioProvider.obtenerAuth(context)
        auth.currentUser?.delete()?.esperar()
    }

    /** Cierra la sesion de la instancia SECUNDARIA. Nunca afecta la sesion del Administrador. */
    fun cerrarSesionSecundaria() {
        FirebaseSecundarioProvider.obtenerAuth(context).signOut()
    }

    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { continuacion ->
        addOnSuccessListener { resultado -> continuacion.resume(resultado) }
        addOnFailureListener { excepcion -> continuacion.resumeWithException(excepcion) }
    }
}
