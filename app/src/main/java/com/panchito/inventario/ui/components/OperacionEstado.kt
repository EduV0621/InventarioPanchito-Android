package com.panchito.inventario.ui.components

/**
 * Estado de una accion puntual del usuario (guardar, eliminar...), distinto del UiState de una pantalla
 * que carga datos. La UI reacciona a Exitosa/Fallida y luego avisa al ViewModel para volver a Inactiva.
 */
sealed interface OperacionEstado {
    data object Inactiva : OperacionEstado
    data object EnProgreso : OperacionEstado
    data class Exitosa(val mensaje: String) : OperacionEstado
    data class Fallida(val mensaje: String) : OperacionEstado
}
