package com.panchito.inventario.ui.screens.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.MovimientoRepository
import com.panchito.inventario.data.repository.ProductoRepository
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.domain.model.TipoMovimiento
import com.panchito.inventario.ui.components.OperacionEstado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class MovimientoFormErrores(
    val producto: String? = null,
    val cantidad: String? = null,
    val motivo: String? = null
) {
    val hayErrores: Boolean get() = listOf(producto, cantidad, motivo).any { it != null }
}

data class MovimientoFormUiState(
    val tipo: TipoMovimiento = TipoMovimiento.ENTRADA,
    val productoId: String? = null,
    val cantidad: String = "",
    val motivo: String? = null,

    val fechaVencimiento: Date? = null,
    val errores: MovimientoFormErrores = MovimientoFormErrores()
)

class MovimientoFormViewModel(
    private val movimientoRepository: MovimientoRepository,
    productoRepository: ProductoRepository
) : ViewModel() {
    private val _formulario = MutableStateFlow(MovimientoFormUiState())
    val formulario: StateFlow<MovimientoFormUiState> = _formulario.asStateFlow()

    private val _operacion = MutableStateFlow<OperacionEstado>(OperacionEstado.Inactiva)
    val operacion: StateFlow<OperacionEstado> = _operacion.asStateFlow()

    val productos: StateFlow<List<Producto>> = productoRepository.observarCatalogo()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val productoSeleccionado: StateFlow<Producto?> = combine(productos, formulario) { lista, f ->
        lista.firstOrNull { it.id == f.productoId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onTipoChange(tipo: TipoMovimiento) = _formulario.update {
        it.copy(tipo = tipo, motivo = null, errores = it.errores.copy(motivo = null))
    }

    fun onProductoChange(id: String) = _formulario.update {
        it.copy(productoId = id, errores = it.errores.copy(producto = null))
    }

    fun onCantidadChange(valor: String) {
        if (valor.length > 9 || !valor.all { it.isDigit() }) return
        _formulario.update { it.copy(cantidad = valor, errores = it.errores.copy(cantidad = null)) }
    }

    fun onMotivoChange(motivo: String) = _formulario.update {
        it.copy(motivo = motivo, errores = it.errores.copy(motivo = null))
    }

    fun onFechaVencimientoChange(fecha: Date?) = _formulario.update { it.copy(fechaVencimiento = fecha) }

    fun registrar() {
        if (_operacion.value is OperacionEstado.EnProgreso) return

        val f = _formulario.value
        val errores = validar(f)
        if (errores.hayErrores) {
            _formulario.update { it.copy(errores = errores) }
            return
        }

        viewModelScope.launch {
            _operacion.value = OperacionEstado.EnProgreso
            movimientoRepository.registrar(
                productoId = f.productoId.orEmpty(),
                tipo = f.tipo,
                cantidad = f.cantidad.toInt(),
                motivo = f.motivo.orEmpty(),
                fechaVencimiento = f.fechaVencimiento
            )
                .onSuccess { nuevoStock ->
                    val etiqueta = if (f.tipo == TipoMovimiento.ENTRADA) "Entrada" else "Salida"
                    _operacion.value = OperacionEstado.Exitosa(
                        "$etiqueta registrada. Stock actualizado: $nuevoStock."
                    )
                }
                .onFailure { error ->

                    _operacion.value = OperacionEstado.Fallida(
                        error.message ?: "No se pudo registrar el movimiento."
                    )
                }
        }
    }

    fun operacionConsumida() {
        _operacion.value = OperacionEstado.Inactiva
    }

    private fun validar(f: MovimientoFormUiState): MovimientoFormErrores {
        val cantidad = f.cantidad.toIntOrNull()
        return MovimientoFormErrores(
            producto = if (f.productoId.isNullOrBlank()) "Selecciona un producto" else null,
            cantidad = when {
                f.cantidad.isBlank() -> "Ingresa la cantidad"
                cantidad == null -> "Ingresa un número válido"
                cantidad <= 0 -> "La cantidad debe ser mayor que 0"
                else -> null
            },
            motivo = if (f.motivo.isNullOrBlank()) "Selecciona un motivo" else null
        )
    }
}
