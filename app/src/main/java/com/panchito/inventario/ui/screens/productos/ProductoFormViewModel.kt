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

/**
 * Valores del formulario tal como los escribe el usuario (texto), mas los errores de validacion
 * por campo.
 *
 * El codigo YA NO se escribe aca: lo genera automaticamente el backend/repositorio al registrar
 * (ver ProductoRepositoryImpl.generarSiguienteCodigo); al editar se conserva el del producto
 * original sin mostrarlo en el formulario.
 */
data class ProductoFormUiState(
    val nombre: String = "",
    val categoriaId: Long? = null,
    val precio: String = "",
    val stock: String = "",
    val stockMinimo: String = "",
    val fechaVencimiento: Date? = null,
    val errores: ProductoFormErrores = ProductoFormErrores()
)

/**
 * ViewModel del formulario de producto. Sirve para REGISTRAR (productoId == null) y para EDITAR.
 */
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

    /** No nulo si el producto a editar no se pudo cargar (no existe o fallo la lectura). */
    private val _errorCarga = MutableStateFlow<String?>(null)
    val errorCarga: StateFlow<String?> = _errorCarga.asStateFlow()

    private val _operacion = MutableStateFlow<OperacionEstado>(OperacionEstado.Inactiva)
    val operacion: StateFlow<OperacionEstado> = _operacion.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = categoriaRepository.observarCategorias()
        .map { lista -> lista.filter { it.activa } }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Producto original al editar: conserva id y estado "activo", que el formulario no modifica. */
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
            // Al registrar el codigo real lo genera ProductoRepositoryImpl (PRO0001, PRO0002...);
            // este valor se ignora. Al editar se conserva el del producto original: el formulario
            // ya no lo expone para modificarlo.
            codigo = original?.codigo.orEmpty(),
            nombre = f.nombre.trim(),
            categoriaId = categoriaId,
            precio = f.precio.toDouble(),
            // Al registrar el stock siempre nace en 0 (se carga despues con un movimiento de
            // ENTRADA); al editar se respeta lo que el usuario haya escrito en el campo.
            stock = if (esEdicion) f.stock.toInt() else 0,
            stockMinimo = f.stockMinimo.toInt(),
            fechaVencimiento = f.fechaVencimiento,
            activo = original?.activo ?: true
        )

        viewModelScope.launch {
            _operacion.value = OperacionEstado.EnProgreso
            // Room siempre se guarda primero; el mensaje agrega ademas lo ocurrido en la nube (Firestore).
            val resultado: Result<String> =
                if (original == null) {
                    productoRepository.registrar(producto).map { it.nube.conMensajeDeNube("Producto registrado.") }
                } else {
                    productoRepository.actualizar(producto).map { it.nube.conMensajeDeNube("Producto actualizado.") }
                }

            resultado
                .onSuccess { mensaje -> _operacion.value = OperacionEstado.Exitosa(mensaje) }
                .onFailure { error ->
                    // El codigo ya no lo escribe el usuario (se genera solo al registrar), asi
                    // que un CodigoDuplicado no tiene un campo del formulario donde mostrarse: se
                    // informa como error general. En la practica solo puede pasar por el caso
                    // documentado en ProductoRepositoryImpl.generarSiguienteCodigo (colision entre
                    // dos dispositivos sin conexion al mismo tiempo).
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
            // Al registrar el stock no se pide (siempre nace en 0); solo se valida al editar.
            stock = if (esEdicion && f.stock.toIntOrNull() == null) "Ingresa el stock" else null,
            stockMinimo = if (f.stockMinimo.toIntOrNull() == null) "Ingresa el stock minimo" else null
        )
    }
}
