package com.panchito.inventario.ui.components

sealed interface OperacionEstado {
    data object Inactiva : OperacionEstado
    data object EnProgreso : OperacionEstado
    data class Exitosa(val mensaje: String) : OperacionEstado
    data class Fallida(val mensaje: String) : OperacionEstado
}
