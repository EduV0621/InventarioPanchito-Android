package com.panchito.inventario.ui.screens.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.MovimientoRepository
import com.panchito.inventario.domain.model.MovimientoDetalle
import com.panchito.inventario.ui.components.UiState
import kotlinx.coroutines.flow.*

/**
 * ViewModel de Movimientos de inventario (PB09/PB10).
 * Fase 5.2: el historial se lee de Room a traves de [MovimientoRepository] y solo contiene los
 * movimientos del usuario autenticado. El registro de entradas/salidas vive en
 * [MovimientoFormViewModel].
 */
class MovimientosViewModel(
    movimientoRepository: MovimientoRepository
) : ViewModel() {

    val estado: StateFlow<UiState<List<MovimientoDetalle>>> = movimientoRepository.observarMovimientos()
        .map { movimientos -> if (movimientos.isEmpty()) UiState.Empty else UiState.Success(movimientos) }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar los movimientos")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
