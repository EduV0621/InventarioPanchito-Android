package com.panchito.inventario.ui.screens.productos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.ui.components.CampoFecha
import com.panchito.inventario.ui.components.OperacionEstado
import com.panchito.inventario.ui.productoFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormScreen(
    productoId: String,
    onVolver: () -> Unit,
    viewModel: ProductoFormViewModel = productoFormViewModel(productoId)
) {
    val formulario by viewModel.formulario.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val errorCarga by viewModel.errorCarga.collectAsState()
    val operacion by viewModel.operacion.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val contexto = LocalContext.current
    var mostrarConfirmacionGuardar by remember { mutableStateOf(false) }

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

    val guardando = operacion is OperacionEstado.EnProgreso

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.esEdicion) "Editar producto" else "Nuevo producto") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            cargando -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            errorCarga != null -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorCarga ?: "", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onVolver) { Text("Volver") }
            }

            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp)
            ) {
                CampoFormulario(
                    valor = formulario.nombre,
                    onCambio = viewModel::onNombreChange,
                    etiqueta = "Nombre",
                    error = formulario.errores.nombre
                )
                Spacer(Modifier.height(12.dp))
                SelectorCategoria(
                    categorias = categorias,
                    seleccionadaId = formulario.categoriaId,
                    error = formulario.errores.categoria,
                    onSeleccion = viewModel::onCategoriaChange
                )
                Spacer(Modifier.height(12.dp))
                CampoFormulario(
                    valor = formulario.precio,
                    onCambio = viewModel::onPrecioChange,
                    etiqueta = "Precio (S/)",
                    error = formulario.errores.precio,
                    teclado = KeyboardType.Decimal
                )

                if (viewModel.esEdicion) {
                    Spacer(Modifier.height(12.dp))
                    CampoFormulario(
                        valor = formulario.stock,
                        onCambio = viewModel::onStockChange,
                        etiqueta = "Stock",
                        error = formulario.errores.stock,
                        teclado = KeyboardType.Number
                    )
                }
                Spacer(Modifier.height(12.dp))
                CampoFormulario(
                    valor = formulario.stockMinimo,
                    onCambio = viewModel::onStockMinimoChange,
                    etiqueta = "Stock minimo (alerta de stock bajo)",
                    error = formulario.errores.stockMinimo,
                    teclado = KeyboardType.Number
                )

                if (viewModel.esEdicion) {
                    Spacer(Modifier.height(12.dp))
                    CampoFecha(
                        fecha = formulario.fechaVencimiento,
                        onCambio = viewModel::onFechaVencimientoChange
                    )
                }
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { mostrarConfirmacionGuardar = true },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (viewModel.esEdicion) "Guardar cambios" else "Registrar producto")
                    }
                }
            }
        }
    }

    if (mostrarConfirmacionGuardar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionGuardar = false },
            title = { Text(if (viewModel.esEdicion) "Guardar cambios" else "Registrar producto") },
            text = {
                Text(
                    if (viewModel.esEdicion) {
                        "¿Estás seguro de guardar los cambios de este producto?"
                    } else {
                        "¿Estás seguro de registrar este producto nuevo?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionGuardar = false
                    viewModel.guardar()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionGuardar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CampoFormulario(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = { if (error != null) Text(error) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCategoria(
    categorias: List<Categoria>,
    seleccionadaId: Long?,
    error: String?,
    onSeleccion: (Long) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val nombreSeleccionado = categorias.firstOrNull { it.id == seleccionadaId }?.nombre ?: ""

    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = nombreSeleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            isError = error != null,
            supportingText = { if (error != null) Text(error) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            categorias.forEach { categoria ->
                DropdownMenuItem(
                    text = { Text(categoria.nombre) },
                    onClick = {
                        onSeleccion(categoria.id)
                        expandido = false
                    }
                )
            }
        }
    }
}
