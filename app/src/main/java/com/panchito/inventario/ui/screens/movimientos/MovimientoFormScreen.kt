package com.panchito.inventario.ui.screens.movimientos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.panchito.inventario.domain.model.MotivosMovimiento
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.domain.model.TipoMovimiento
import com.panchito.inventario.ui.components.CampoFecha
import com.panchito.inventario.ui.components.OperacionEstado
import com.panchito.inventario.ui.movimientoFormViewModel

/**
 * Formulario de registro de un movimiento de inventario (Fase 5.2).
 * El producto se elige de un desplegable con el catalogo del usuario: no se puede escribir un
 * producto inexistente. Al registrar con exito vuelve al historial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientoFormScreen(
    onVolver: () -> Unit,
    viewModel: MovimientoFormViewModel = movimientoFormViewModel()
) {
    val formulario by viewModel.formulario.collectAsState()
    val operacion by viewModel.operacion.collectAsState()
    val productos by viewModel.productos.collectAsState()
    val seleccionado by viewModel.productoSeleccionado.collectAsState()
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
                // Aqui cae, entre otros, "Stock insuficiente. Stock disponible: N."
                snackbarHostState.showSnackbar(actual.mensaje)
                viewModel.operacionConsumida()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar movimiento") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tipo de movimiento", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TipoMovimiento.entries.forEachIndexed { indice, tipo ->
                    SegmentedButton(
                        selected = formulario.tipo == tipo,
                        onClick = { viewModel.onTipoChange(tipo) },
                        shape = SegmentedButtonDefaults.itemShape(index = indice, count = TipoMovimiento.entries.size)
                    ) {
                        Text(if (tipo == TipoMovimiento.ENTRADA) "Entrada" else "Salida")
                    }
                }
            }

            SelectorProducto(
                productos = productos,
                seleccionadoId = formulario.productoId,
                error = formulario.errores.producto,
                onSeleccion = viewModel::onProductoChange
            )

            OutlinedTextField(
                value = seleccionado?.stock?.toString() ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Stock actual") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formulario.cantidad,
                onValueChange = viewModel::onCantidadChange,
                label = { Text("Cantidad") },
                isError = formulario.errores.cantidad != null,
                supportingText = { formulario.errores.cantidad?.let { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            SelectorMotivo(
                motivos = MotivosMovimiento.de(formulario.tipo),
                seleccionado = formulario.motivo,
                error = formulario.errores.motivo,
                onSeleccion = viewModel::onMotivoChange
            )

            // Opcional: hay productos que no vencen (ej. limpieza). Si se elige una fecha aqui,
            // actualiza la fecha de vencimiento del producto junto con este movimiento.
            CampoFecha(
                fecha = formulario.fechaVencimiento,
                onCambio = viewModel::onFechaVencimientoChange
            )

            Button(
                onClick = { mostrarConfirmacion = true },
                enabled = operacion !is OperacionEstado.EnProgreso,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrar movimiento")
            }
        }
    }

    if (mostrarConfirmacion) {
        val etiquetaTipo = if (formulario.tipo == TipoMovimiento.ENTRADA) "entrada" else "salida"
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Registrar movimiento") },
            text = { Text("¿Estás seguro de registrar esta $etiquetaTipo de inventario?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    viewModel.registrar()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorProducto(
    productos: List<Producto>,
    seleccionadoId: String?,
    error: String?,
    onSeleccion: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val nombre = productos.firstOrNull { it.id == seleccionadoId }?.nombre ?: ""

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = nombre,
            onValueChange = {},
            readOnly = true,
            label = { Text("Producto") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = error != null,
            supportingText = {
                when {
                    error != null -> Text(error)
                    productos.isEmpty() -> Text("Registra un producto en el catálogo primero")
                }
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            productos.forEach { producto ->
                DropdownMenuItem(
                    text = { Text("${producto.nombre} (stock: ${producto.stock})") },
                    onClick = {
                        onSeleccion(producto.id)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorMotivo(
    motivos: List<String>,
    seleccionado: String?,
    error: String?,
    onSeleccion: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = seleccionado ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Motivo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = error != null,
            supportingText = { if (error != null) Text(error) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            motivos.forEach { motivo ->
                DropdownMenuItem(
                    text = { Text(motivo) },
                    onClick = {
                        onSeleccion(motivo)
                        expandido = false
                    }
                )
            }
        }
    }
}
