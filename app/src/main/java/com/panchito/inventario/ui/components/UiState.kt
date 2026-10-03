package com.panchito.inventario.ui.components

/**
 * Estado generico de pantalla, expuesto por los ViewModel via StateFlow
 * (ver informe, seccion Arquitectura MVVM: "Loading, Success, Error, Empty, Offline, Syncing").
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val mensaje: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data object Offline : UiState<Nothing>
    data object Syncing : UiState<Nothing>
}
