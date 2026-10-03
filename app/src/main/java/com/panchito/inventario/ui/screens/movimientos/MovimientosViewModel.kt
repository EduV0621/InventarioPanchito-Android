package com.panchito.inventario.ui.screens.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.MovimientoRepository
import com.panchito.inventario.domain.model.MovimientoDetalle
import com.panchito.inventario.ui.components.UiState
import kotlinx.coroutines.flow.*

class MovimientosViewModel(
    movimientoRepository: MovimientoRepository
) : ViewModel() {
    val estado: StateFlow<UiState<List<MovimientoDetalle>>> = movimientoRepository.observarMovimientos()
        .map { movimientos -> if (movimientos.isEmpty()) UiState.Empty else UiState.Success(movimientos) }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar los movimientos")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
