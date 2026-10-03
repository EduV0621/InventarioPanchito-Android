package com.panchito.inventario.ui.screens.productos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.panchito.inventario.ui.components.OperacionEstado
import com.panchito.inventario.ui.components.UiState
import com.panchito.inventario.ui.components.UiStateWrapper
import com.panchito.inventario.ui.components.comoFecha
import com.panchito.inventario.ui.components.comoPrecio
import com.panchito.inventario.ui.productoDetalleViewModel
import com.panchito.inventario.ui.theme.PanchitoAmber
import com.panchito.inventario.ui.theme.PanchitoGreen
import com.panchito.inventario.ui.theme.PanchitoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoDetalleScreen(
    productoId: String,
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    viewModel: ProductoDetalleViewModel = productoDetalleViewModel(productoId)
) {
    val estado by viewModel.estado.collectAsState()
    val operacion by viewModel.operacion.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val contexto = LocalContext.current
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    LaunchedEffect(operacion) {
        when (val actual = operacion) {
            is OperacionEstado.Exitosa -> {
                Toast.makeText(contexto, actual.mensaje, Toast.LENGTH_LONG).show()
                viewModel.operacionConsumida()
                onVolver()
            }
            is OperacionEstado.Fallida -> {
                snackbarHostState.showSnackbar(actual.mensaje)
                viewModel.operacionConsumida()
            }
            else -> Unit
        }
    }

    val detalle = (estado as? UiState.Success)?.data
    val ocupado = operacion is OperacionEstado.EnProgreso

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle del producto") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (detalle != null) {
                        IconButton(onClick = onEditar, enabled = !ocupado) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar producto")
                        }
                        IconButton(onClick = { mostrarConfirmacion = true }, enabled = !ocupado) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar producto")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            UiStateWrapper(state = estado, mensajeVacio = "El producto no existe o ya fue eliminado.") { datos ->
                val producto = datos.producto
                val colorEstado = when {
                    producto.agotado -> PanchitoRed
                    producto.stockBajo -> PanchitoAmber
                    else -> PanchitoGreen
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(producto.nombre, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            producto.agotado -> "Agotado"
                            producto.stockBajo -> "Stock bajo"
                            else -> "Stock disponible"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colorEstado
                    )
                    Spacer(Modifier.height(16.dp))
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilaDato("Codigo", producto.codigo)
                            FilaDato("Categoria", datos.categoria)
                            FilaDato("Precio", producto.precio.comoPrecio())
                            FilaDato("Stock actual", producto.stock.toString())
                            FilaDato("Stock minimo", producto.stockMinimo.toString())
                            FilaDato("Vencimiento", producto.fechaVencimiento?.comoFecha() ?: "No aplica")
                        }
                    }
                    if (ocupado) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Eliminar producto") },
            text = {
                Text("Se eliminara \"${detalle?.producto?.nombre ?: "este producto"}\". Esta accion no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    viewModel.eliminar()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
