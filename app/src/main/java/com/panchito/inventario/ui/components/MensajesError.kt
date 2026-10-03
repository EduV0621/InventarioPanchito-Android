package com.panchito.inventario.ui.components

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.panchito.inventario.data.remote.RemotoException
import com.panchito.inventario.data.repository.EstadoNube
import com.panchito.inventario.domain.model.ProductoException
import java.io.IOException

fun Throwable.mensajeParaUsuario(accion: String): String =
    if (this is ProductoException) (message ?: "No se pudo $accion el producto.")
    else "No se pudo $accion el producto. Intenta nuevamente."

fun EstadoNube.conMensajeDeNube(mensajeLocal: String): String = when (this) {
    EstadoNube.GUARDADO -> "$mensajeLocal Cambio guardado en el servidor."
    EstadoNube.SIN_CONEXION -> "$mensajeLocal No hay conexión al servidor: el cambio no se guardó ahí todavía."
    EstadoNube.SIN_SESION -> "$mensajeLocal Debes iniciar sesión para guardar en el servidor."
    EstadoNube.ERROR -> "$mensajeLocal No se pudo guardar el cambio en el servidor."
}

fun Throwable.mensajeDeAutenticacion(): String = when (this) {
    is FirebaseAuthInvalidCredentialsException -> "Correo o contrasena incorrectos."
    is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
    is FirebaseAuthUserCollisionException -> "El correo ya esta registrado."
    is FirebaseAuthWeakPasswordException -> "La contrasena no cumple los requisitos minimos de seguridad."
    is FirebaseNetworkException -> "No hay conexion a Internet."
    else -> "No se pudo completar la autenticacion. Intentalo nuevamente."
}

const val MENSAJE_SIN_CONEXION_REGISTRO_EMPLEADO =
    "No hay conexión: el registro de empleados requiere Internet. Conéctate e inténtalo de nuevo."

private fun Throwable.esErrorDeRed(): Boolean {
    if (this is RemotoException) return this is RemotoException.SinConexion

    var actual: Throwable? = this
    var profundidad = 0
    while (actual != null && profundidad < 5) {
        if (actual is FirebaseNetworkException || actual is IOException) return true
        actual = actual.cause
        profundidad++
    }

    val texto = message.orEmpty()
    return listOf("network error", "unable to resolve host", "failed to connect", "timeout")
        .any { texto.contains(it, ignoreCase = true) }
}

fun Throwable.mensajeDeRegistroEmpleado(): String = when {
    esErrorDeRed() -> MENSAJE_SIN_CONEXION_REGISTRO_EMPLEADO
    this is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
    this is FirebaseAuthWeakPasswordException -> "La contraseña no cumple los requisitos mínimos de seguridad."
    this is FirebaseAuthInvalidCredentialsException -> "El correo electrónico no tiene un formato válido."
    this is RemotoException -> message ?: "No se pudo registrar el empleado en el servidor."

    else -> "No se pudo registrar el empleado. Intenta nuevamente."
}
