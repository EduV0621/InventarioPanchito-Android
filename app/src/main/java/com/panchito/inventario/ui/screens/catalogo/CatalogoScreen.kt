package com.panchito.inventario.ui.screens.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.ui.catalogoViewModel
import com.panchito.inventario.ui.components.UiState
import com.panchito.inventario.ui.components.UiStateWrapper
import com.panchito.inventario.ui.components.comoPrecio
import com.panchito.inventario.ui.theme.PanchitoAmber
import com.panchito.inventario.ui.theme.PanchitoGreen
import com.panchito.inventario.ui.theme.PanchitoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    esAdministrador: Boolean,
    onVolver: () -> Unit,
    onNuevoProducto: () -> Unit,
    onVerProducto: (String) -> Unit,
    viewModel: CatalogoViewModel = catalogoViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val consulta by viewModel.consulta.collectAsState()
    val focoActual = LocalFocusManager.current
    val buscando = consulta.isNotBlank()

    var expandidas by rememberSaveable { mutableStateOf(emptySet<Long>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catalogo de productos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (esAdministrador) {
                FloatingActionButton(onClick = onNuevoProducto) {
                    Icon(Icons.Filled.Add, contentDescription = "Nuevo producto")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (estado is UiState.Success) {
                OutlinedTextField(
                    value = consulta,
                    onValueChange = viewModel::onConsultaChange,
                    placeholder = { Text("Buscar por nombre o código") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (consulta.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onConsultaChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),

                    keyboardActions = KeyboardActions(onSearch = { focoActual.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                UiStateWrapper(
                    state = estado,
                    mensajeVacio = "Aun no hay productos registrados. Toca el boton + para agregar el primero."
                ) { grupos ->
                    if (grupos.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "No se encontraron productos para \"${consulta.trim()}\".",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Prueba con otro nombre o código.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grupos.forEach { grupo ->

                                val expandida = buscando || grupo.id in expandidas
                                item(key = "categoria-${grupo.id}") {
                                    CabeceraCategoria(
                                        grupo = grupo,
                                        expandida = expandida,
                                        buscando = buscando,
                                        onClick = { expandidas = if (expandida) expandidas - grupo.id else expandidas + grupo.id }
                                    )
                                }
                                if (expandida) {
                                    if (grupo.productos.isEmpty()) {
                                        item(key = "vacia-${grupo.id}") {
                                            Text(
                                                "No hay productos en esta categoría.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                            )
                                        }
                                    } else {
                                        items(grupo.productos, key = { it.id }) { producto ->
                                            FilaProducto(
                                                producto = producto,
                                                onClick = { onVerProducto(producto.id) },
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CabeceraCategoria(grupo: GrupoCategoria, expandida: Boolean, buscando: Boolean, onClick: () -> Unit) {
    val modificador = if (buscando) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().clickable(onClick = onClick)
    ElevatedCard(modifier = modificador) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(grupo.nombre, style = MaterialTheme.typography.titleMedium)
                val cantidad = grupo.productos.size
                val conteo = when {
                    buscando -> if (cantidad == 1) "1 coincidencia" else "$cantidad coincidencias"
                    cantidad == 1 -> "1 producto"
                    else -> "$cantidad productos"
                }
                Text(
                    text = conteo + if (grupo.activa) "" else " · Categoría inactiva",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!buscando) {
                Icon(
                    imageVector = if (expandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expandida) "Contraer ${grupo.nombre}" else "Desplegar ${grupo.nombre}"
                )
            }
        }
    }
}

@Composable
private fun FilaProducto(producto: Producto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colorEstado = when {
                producto.agotado -> PanchitoRed
                producto.stockBajo -> PanchitoAmber
                else -> PanchitoGreen
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(colorEstado)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                Text("Codigo: ${producto.codigo}", style = MaterialTheme.typography.bodyMedium)

                if (producto.estadoSincronizacion.estaPendiente) {
                    Text(
                        "Pendiente de sincronización",
                        style = MaterialTheme.typography.labelSmall,
                        color = PanchitoAmber
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(producto.precio.comoPrecio(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when {
                        producto.agotado -> "Agotado"
                        producto.stockBajo -> "Stock bajo (${producto.stock})"
                        else -> "Stock: ${producto.stock}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorEstado
                )
            }
        }
    }
}
