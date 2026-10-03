package com.panchito.inventario.ui.screens.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.CategoriaRepository
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.domain.model.CategoriaException
import com.panchito.inventario.ui.components.OperacionEstado
import com.panchito.inventario.ui.components.conMensajeDeNube
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla "Gestionar categorias" (solo Administrador):
 * permite registrar categorias nuevas, editar su nombre y activarlas/desactivarlas.
 * Nunca se elimina una categoria de la base de datos (borrado logico, ver CategoriaRepository).
 */
class CategoriasViewModel(
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    val categorias: StateFlow<List<Categoria>> = categoriaRepository.observarCategorias()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _operacion = MutableStateFlow<OperacionEstado>(OperacionEstado.Inactiva)
    val operacion: StateFlow<OperacionEstado> = _operacion.asStateFlow()

    fun registrar(nombre: String) {
        if (_operacion.value is OperacionEstado.EnProgreso) return
        viewModelScope.launch {
            _operacion.value = OperacionEstado.EnProgreso
            categoriaRepository.registrar(nombre)
                .onSuccess { _operacion.value = OperacionEstado.Exitosa(it.nube.conMensajeDeNube("Categoría registrada.")) }
                .onFailure { _operacion.value = OperacionEstado.Fallida(it.mensajeDeCategoria("No se pudo registrar la categoría.")) }
        }
    }

    fun actualizar(id: Long, nombre: String) {
        if (_operacion.value is OperacionEstado.EnProgreso) return
        viewModelScope.launch {
            _operacion.value = OperacionEstado.EnProgreso
            categoriaRepository.actualizar(id, nombre)
                .onSuccess { _operacion.value = OperacionEstado.Exitosa(it.nube.conMensajeDeNube("Categoría actualizada.")) }
                .onFailure { _operacion.value = OperacionEstado.Fallida(it.mensajeDeCategoria("No se pudo actualizar la categoría.")) }
        }
    }

    fun cambiarEstado(id: Long, activa: Boolean) {
        if (_operacion.value is OperacionEstado.EnProgreso) return
        viewModelScope.launch {
            _operacion.value = OperacionEstado.EnProgreso
            categoriaRepository.cambiarEstado(id, activa)
                .onSuccess {
                    val mensaje = if (activa) "Categoría activada." else "Categoría desactivada."
                    _operacion.value = OperacionEstado.Exitosa(it.nube.conMensajeDeNube(mensaje))
                }
                .onFailure { _operacion.value = OperacionEstado.Fallida(it.mensajeDeCategoria("No se pudo cambiar el estado.")) }
        }
    }

    fun operacionConsumida() {
        _operacion.value = OperacionEstado.Inactiva
    }

    /** Solo se muestran mensajes propios del dominio (nombre vacio/duplicado...); nada tecnico. */
    private fun Throwable.mensajeDeCategoria(porDefecto: String): String =
        if (this is CategoriaException) message ?: porDefecto else porDefecto
}
