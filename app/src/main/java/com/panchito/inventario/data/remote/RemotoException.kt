package com.panchito.inventario.data.remote

sealed class RemotoException(mensaje: String) : Exception(mensaje) {
    class SinConexion : RemotoException("Sin conexión al servidor. Se muestran los datos guardados localmente.")

    class ErrorServidor(val codigoHttp: Int, mensajeServidor: String? = null) :
        RemotoException(
            mensajeServidor?.takeIf { it.isNotBlank() }
                ?: "El servidor respondió con un error (código $codigoHttp). Intenta nuevamente."
        )

    class Desconocido(mensaje: String) :
        RemotoException(mensaje.ifBlank { "Ocurrió un error inesperado al conectar con el servidor." })
}
