package com.panchito.inventario.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.ProductoRepository
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.ui.components.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ResumenDashboard(
    val totalProductos: Int,
    val stockBajo: Int,
    val agotados: Int
)

class DashboardViewModel(
    productoRepository: ProductoRepository
) : ViewModel() {
    val estado: StateFlow<UiState<ResumenDashboard>> = productoRepository.observarCatalogo()
        .map<List<Producto>, UiState<ResumenDashboard>> { productos ->
            UiState.Success(
                ResumenDashboard(
                    totalProductos = productos.size,
                    stockBajo = productos.count { it.stockBajo },
                    agotados = productos.count { it.agotado }
                )
            )
        }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar el resumen")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
