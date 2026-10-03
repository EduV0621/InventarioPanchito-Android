package com.panchito.inventario.ui.screens.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.CategoriaRepository
import com.panchito.inventario.data.repository.ProductoRepository
import com.panchito.inventario.data.repository.ResultadoEliminacion
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.ui.components.OperacionEstado
import com.panchito.inventario.ui.components.UiState
import com.panchito.inventario.ui.components.conMensajeDeNube
import com.panchito.inventario.ui.components.mensajeParaUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductoDetalle(
    val producto: Producto,
    val categoria: String
)

/** ViewModel de la pantalla de detalle: muestra un producto y permite eliminarlo. */
class ProductoDetalleViewModel(
    private val productoId: String,
    private val productoRepository: ProductoRepository,
    categoriaRepository: CategoriaRepository
) : ViewModel() {

    val estado: StateFlow<UiState<ProductoDetalle>> = combine(
        productoRepository.observarProducto(productoId),
        categoriaRepository.observarCategorias()
    ) { producto: Producto?, categorias: List<Categoria> ->
        val estadoPantalla: UiState<ProductoDetalle> =
            if (producto == null) {
                UiState.Empty
            } else {
                val nombreCategoria = categorias.firstOrNull { it.id == producto.categoriaId }?.nombre ?: "Sin categoria"
                UiState.Success(ProductoDetalle(producto, nombreCategoria))
            }
        estadoPantalla
    }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar el producto")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _operacion = MutableStateFlow<OperacionEstado>(OperacionEstado.Inactiva)
    val operacion: StateFlow<OperacionEstado> = _operacion.asStateFlow()

    fun eliminar() {
        if (_operacion.value is OperacionEstado.EnProgreso) return
        viewModelScope.launch {
            _operacion.value = OperacionEstado.EnProgreso
            productoRepository.eliminar(productoId)
                .onSuccess { resultado ->
                    val mensajeLocal =
                        if (resultado.valor == ResultadoEliminacion.DESACTIVADO) {
                            "Producto desactivado: tiene movimientos asociados."
                        } else {
                            "Producto eliminado."
                        }
                    _operacion.value = OperacionEstado.Exitosa(resultado.nube.conMensajeDeNube(mensajeLocal))
                }
                .onFailure { error ->
                    _operacion.value = OperacionEstado.Fallida(error.mensajeParaUsuario("eliminar"))
                }
        }
    }

    /** La UI ya mostro el resultado (mensaje / navegacion): vuelve al estado inicial. */
    fun operacionConsumida() {
        _operacion.value = OperacionEstado.Inactiva
    }
}
