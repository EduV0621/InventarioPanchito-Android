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

/** Id reservado para el grupo de productos cuya categoria ya no se encuentra (nunca coincide con una real). */
const val ID_SIN_CATEGORIA = -1L

private const val LARGO_MAXIMO_BUSQUEDA = 60

/** Una categoria del catalogo con los productos que contiene (se muestra como seccion desplegable). */
data class GrupoCategoria(
    val id: Long,
    val nombre: String,
    val activa: Boolean,
    val productos: List<Producto>
)

/**
 * ViewModel del Catalogo de productos (PB06).
 * Expone el catalogo AGRUPADO POR CATEGORIA como UiState (Loading / Success / Empty / Error) y el
 * texto del BUSCADOR. Se actualiza solo: Room emite una lista nueva cada vez que cambia un producto
 * o una categoria, y cada letra escrita en el buscador vuelve a filtrar.
 *
 * - Empty   = todavia no hay ningun producto registrado (no tiene sentido mostrar el buscador).
 * - Success = hay productos; la lista de grupos queda vacia si la busqueda no encontro nada.
 */
class CatalogoViewModel(
    productoRepository: ProductoRepository,
    categoriaRepository: CategoriaRepository
) : ViewModel() {

    // Se declara antes que [estado], que lo usa. Vive en el ViewModel: el texto buscado se conserva
    // al girar la pantalla y al volver del detalle de un producto.
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

    /**
     * Busqueda por nombre o codigo: ignora mayusculas y tildes ("lacteo" encuentra "Lácteo") y
     * acepta varias palabras en cualquier orden ("gloria leche" encuentra "Leche Gloria"). Un texto
     * vacio (o solo espacios) no filtra nada: se muestra el catalogo completo.
     */
    private fun filtrarYAgrupar(productos: List<Producto>, categorias: List<Categoria>, texto: String): List<GrupoCategoria> {
        val terminos = texto.sinTildesNiMayusculas().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terminos.isEmpty()) return agrupar(productos, categorias, soloConProductos = false)

        val coincidencias = productos.filter { producto ->
            val contenido = "${producto.nombre} ${producto.codigo}".sinTildesNiMayusculas()
            terminos.all { it in contenido }
        }
        return agrupar(coincidencias, categorias, soloConProductos = true)
    }

    /**
     * Una seccion por categoria (en orden alfabetico, como ya vienen de Room). Sin busqueda se muestran:
     * - las categorias activas, aunque todavia no tengan productos;
     * - las inactivas solo si aun tienen productos, para que esos productos no desaparezcan.
     * Con busqueda ([soloConProductos]) solo las categorias que tienen coincidencias.
     * Al final, "Sin categoria" si algun producto apunta a una categoria que ya no existe.
     */
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
