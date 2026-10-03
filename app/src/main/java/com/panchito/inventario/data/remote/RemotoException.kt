package com.panchito.inventario.data.remote

/**
 * Errores de la fuente de datos REMOTA (API REST PHP). RetrofitProductoRemoteDataSource traduce
 * aqui las excepciones tecnicas de Retrofit/OkHttp para que el resto de la app (ApiSyncManager,
 * Repository) nunca dependa de esas librerias.
 */
sealed class RemotoException(mensaje: String) : Exception(mensaje) {

    /** Sin Internet o el servidor no respondio (timeout, DNS, XAMPP apagado, etc.). */
    class SinConexion : RemotoException("Sin conexión al servidor. Se muestran los datos guardados localmente.")

    /**
     * El servidor respondio con un codigo de error HTTP (4xx/5xx). [mensajeServidor] es el
     * texto del campo "error" que devuelve productos.php (Response::error), cuando se pudo leer.
     */
    class ErrorServidor(val codigoHttp: Int, mensajeServidor: String? = null) :
        RemotoException(
            mensajeServidor?.takeIf { it.isNotBlank() }
                ?: "El servidor respondió con un error (código $codigoHttp). Intenta nuevamente."
        )

    class Desconocido(mensaje: String) :
        RemotoException(mensaje.ifBlank { "Ocurrió un error inesperado al conectar con el servidor." })
}