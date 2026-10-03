package com.panchito.inventario.ui.screens.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.CategoriaRepository
import com.panchito.inventario.data.repository.ProductoRepository
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.ui.components.OperacionEstado
import com.panchito.inventario.ui.components.comoTextoEditable
import com.panchito.inventario.ui.components.conMensajeDeNube
import com.panchito.inventario.ui.components.mensajeParaUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class ProductoFormErrores(
    val nombre: String? = null,
    val categoria: String? = null,
    val precio: String? = null,
    val stock: String? = null,
    val stockMinimo: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(nombre, categoria, precio, stock, stockMinimo).any { it != null }
}

data class ProductoFormUiState(
    val nombre: String = "",
    val categoriaId: Long? = null,
    val precio: String = "",
    val stock: String = "",
    val stockMinimo: String = "",
    val fechaVencimiento: Date? = null,
    val errores: ProductoFormErrores = ProductoFormErrores()
)

class ProductoFormViewModel(
    private val productoId: String?,
    private val productoRepository: ProductoRepository,
    categoriaRepository: CategoriaRepository
) : ViewModel() {
    val esEdicion: Boolean = productoId != null

    private val _formulario = MutableStateFlow(ProductoFormUiState())
    val formulario: StateFlow<ProductoFormUiState> = _formulario.asStateFlow()

    private val _cargando = MutableStateFlow(productoId != null)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _errorCarga = MutableStateFlow<String?>(null)
    val errorCarga: StateFlow<String?> = _errorCarga.asStateFlow()

    private val _operacion = MutableStateFlow<OperacionEstado>(OperacionEstado.Inactiva)
    val operacion: StateFlow<OperacionEstado> = _operacion.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepository.observarCategorias()
        .map { lista -> lista.filter { it.activa } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var productoOriginal: Producto? = null

    init {
        if (productoId != null) cargarProducto(productoId)
    }

    private fun cargarProducto(id: String) {
        viewModelScope.launch {
            try {
                val producto = productoRepository.obtenerProducto(id)
                if (producto == null) {
                    _errorCarga.value = "El producto no existe"
                } else {
                    productoOriginal = producto
                    _formulario.value = ProductoFormUiState(
                        nombre = producto.nombre,
                        categoriaId = producto.categoriaId,
                        precio = producto.precio.comoTextoEditable(),
                        stock = producto.stock.toString(),
                        stockMinimo = producto.stockMinimo.toString(),
                        fechaVencimiento = producto.fechaVencimiento
                    )
                }
            } catch (e: Exception) {
                _errorCarga.value = "No se pudo cargar el producto"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun onNombreChange(valor: String) = _formulario.update {
        it.copy(nombre = valor, errores = it.errores.copy(nombre = null))
    }

    fun onCategoriaChange(id: Long) = _formulario.update {
        it.copy(categoriaId = id, errores = it.errores.copy(categoria = null))
    }

    fun onPrecioChange(valor: String) {
        val limpio = valor.replace(',', '.')
        if (!Regex("^\\d{0,7}(\\.\\d{0,2})?$").matches(limpio)) return
        _formulario.update { it.copy(precio = limpio, errores = it.errores.copy(precio = null)) }
    }

    fun onStockChange(valor: String) {
        if (valor.length > 9 || !valor.all { it.isDigit() }) return
        _formulario.update { it.copy(stock = valor, errores = it.errores.copy(stock = null)) }
    }

    fun onStockMinimoChange(valor: String) {
        if (valor.length > 9 || !valor.all { it.isDigit() }) return
        _formulario.update { it.copy(stockMinimo = valor, errores = it.errores.copy(stockMinimo = null)) }
    }

    fun onFechaVencimientoChange(fecha: Date?) = _formulario.update { it.copy(fechaVencimiento = fecha) }

    fun guardar() {
        if (_cargando.value || _operacion.value is OperacionEstado.EnProgreso) return

        val f = _formulario.value
        val errores = validar(f)
        val categoriaId = f.categoriaId
        if (errores.hayErrores || categoriaId == null) {
            _formulario.update { it.copy(errores = errores) }
            return
        }

        val original = productoOriginal
        val producto = Producto(
            id = original?.id.orEmpty(),

            codigo = original?.codigo.orEmpty(),
            nombre = f.nombre.trim(),
            categoriaId = categoriaId,
            precio = f.precio.toDouble(),

            stock = if (esEdicion) f.stock.toInt() else 0,
            stockMinimo = f.stockMinimo.toInt(),
            fechaVencimiento = f.fechaVencimiento,
            activo = original?.activo ?: true
        )

        viewModelScope.launch {
            _operacion.value = OperacionEstado.EnProgreso

            val resultado: Result<String> =
                if (original == null) {
                    productoRepository.registrar(producto).map { it.nube.conMensajeDeNube("Producto registrado.") }
                } else {
                    productoRepository.actualizar(producto).map { it.nube.conMensajeDeNube("Producto actualizado.") }
                }

            resultado
                .onSuccess { mensaje -> _operacion.value = OperacionEstado.Exitosa(mensaje) }
                .onFailure { error ->

                    _operacion.value = OperacionEstado.Fallida(
                        error.mensajeParaUsuario(if (original == null) "guardar" else "actualizar")
                    )
                }
        }
    }

    fun operacionConsumida() {
        _operacion.value = OperacionEstado.Inactiva
    }

    private fun validar(f: ProductoFormUiState): ProductoFormErrores {
        val precio = f.precio.toDoubleOrNull()
        return ProductoFormErrores(
            nombre = if (f.nombre.isBlank()) "Ingresa el nombre" else null,
            categoria = if (f.categoriaId == null) "Selecciona una categoria" else null,
            precio = when {
                precio == null -> "Ingresa un precio valido"
                precio < 0 -> "El precio no puede ser negativo"
                else -> null
            },

            stock = if (esEdicion && f.stock.toIntOrNull() == null) "Ingresa el stock" else null,
            stockMinimo = if (f.stockMinimo.toIntOrNull() == null) "Ingresa el stock minimo" else null
        )
    }
}
