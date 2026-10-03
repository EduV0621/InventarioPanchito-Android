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

/**
 * Completa el mensaje de exito de una escritura del inventario real con lo ocurrido en el
 * servidor (API REST PHP + MySQL). Room siempre se guarda primero, asi que un fallo del servidor
 * no es un error de la operacion: se informa junto al exito. Nunca muestra detalles tecnicos.
 */
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

/** Mensaje unico cuando se intenta registrar un empleado sin Internet (ver [esErrorDeRed]). */
const val MENSAJE_SIN_CONEXION_REGISTRO_EMPLEADO =
    "No hay conexión: el registro de empleados requiere Internet. Conéctate e inténtalo de nuevo."

/**
 * true si la causa del fallo es la red (sin Internet, DNS, timeout, servidor inalcanzable).
 * Firebase Authentication no siempre lanza [FirebaseNetworkException] cuando no hay red: a veces
 * llega una FirebaseException generica cuyo texto es "An internal error has occurred. [ Unable to
 * resolve host ... ]", que sin este filtro se mostraria tal cual al usuario.
 */
private fun Throwable.esErrorDeRed(): Boolean {
    // Los errores del backend PHP ya vienen clasificados: solo SinConexion es un problema de red.
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

/**
 * Mensajes para "Registrar empleado" (Fase 1): cubre tanto los errores de Firebase Authentication
 * (creacion de la cuenta) como los del backend PHP (guardado en MySQL, ver EmpleadoRepositoryImpl).
 * Cualquier fallo de red se traduce al mismo mensaje claro, nunca al texto crudo de Firebase.
 */
fun Throwable.mensajeDeRegistroEmpleado(): String = when {
    esErrorDeRed() -> MENSAJE_SIN_CONEXION_REGISTRO_EMPLEADO
    this is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
    this is FirebaseAuthWeakPasswordException -> "La contraseña no cumple los requisitos mínimos de seguridad."
    this is FirebaseAuthInvalidCredentialsException -> "El correo electrónico no tiene un formato válido."
    this is RemotoException -> message ?: "No se pudo registrar el empleado en el servidor."
    // Cualquier otro fallo (mensajes internos de Firebase o de Room) se detalla en Logcat, no al usuario.
    else -> "No se pudo registrar el empleado. Intenta nuevamente."
}
