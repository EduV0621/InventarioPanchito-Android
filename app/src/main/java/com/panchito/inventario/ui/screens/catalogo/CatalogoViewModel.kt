package com.panchito.inventario.ui.screens.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.CategoriaRepository
import com.panchito.inventario.data.repository.ProductoRepository
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.ui.components.UiState
import kotlinx.coroutines.flow.*
import java.text.Normalizer
import java.util.Locale

const val ID_SIN_CATEGORIA = -1L

private const val LARGO_MAXIMO_BUSQUEDA = 60

data class GrupoCategoria(
    val id: Long,
    val nombre: String,
    val activa: Boolean,
    val productos: List<Producto>
)

class CatalogoViewModel(
    productoRepository: ProductoRepository,
    categoriaRepository: CategoriaRepository
) : ViewModel() {
    private val _consulta = MutableStateFlow("")
    val consulta: StateFlow<String> = _consulta.asStateFlow()

    fun onConsultaChange(valor: String) {
        _consulta.value = valor.take(LARGO_MAXIMO_BUSQUEDA)
    }

    val estado: StateFlow<UiState<List<GrupoCategoria>>> = combine(
        productoRepository.observarCatalogo(),
        categoriaRepository.observarCategorias(),
        _consulta
    ) { productos: List<Producto>, categorias: List<Categoria>, texto: String ->
        val estadoPantalla: UiState<List<GrupoCategoria>> =
            if (productos.isEmpty()) UiState.Empty
            else UiState.Success(filtrarYAgrupar(productos, categorias, texto))
        estadoPantalla
    }
        .catch { emit(UiState.Error(it.message ?: "Error al cargar el catalogo")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun filtrarYAgrupar(productos: List<Producto>, categorias: List<Categoria>, texto: String): List<GrupoCategoria> {
        val terminos = texto.sinTildesNiMayusculas().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terminos.isEmpty()) return agrupar(productos, categorias, soloConProductos = false)

        val coincidencias = productos.filter { producto ->
            val contenido = "${producto.nombre} ${producto.codigo}".sinTildesNiMayusculas()
            terminos.all { it in contenido }
        }
        return agrupar(coincidencias, categorias, soloConProductos = true)
    }

    private fun agrupar(productos: List<Producto>, categorias: List<Categoria>, soloConProductos: Boolean): List<GrupoCategoria> {
        val productosPorCategoria = productos.groupBy { it.categoriaId }
        val idsConocidos = categorias.map { it.id }.toSet()

        val grupos = categorias
            .filter { productosPorCategoria.containsKey(it.id) || (!soloConProductos && it.activa) }
            .map { GrupoCategoria(it.id, it.nombre, it.activa, productosPorCategoria[it.id].orEmpty()) }

        val huerfanos = productos.filter { it.categoriaId !in idsConocidos }
        return if (huerfanos.isEmpty()) grupos else grupos + GrupoCategoria(ID_SIN_CATEGORIA, "Sin categoría", true, huerfanos)
    }

    private fun String.sinTildesNiMayusculas(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.getDefault())
}
