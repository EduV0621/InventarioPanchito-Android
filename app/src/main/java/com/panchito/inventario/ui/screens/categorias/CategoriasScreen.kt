package com.panchito.inventario.ui.screens.categorias

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.ui.categoriasViewModel
import com.panchito.inventario.ui.components.OperacionEstado
import com.panchito.inventario.ui.theme.PanchitoAmber

/**
 * Pantalla "Gestionar categorias" (solo Administrador): registrar categorias nuevas, editar su
 * nombre y activarlas/desactivarlas. Nunca se elimina una categoria de la base de datos: la
 * "eliminacion" siempre es un cambio de estado (borrado logico).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    onVolver: () -> Unit,
    viewModel: CategoriasViewModel = categoriasViewModel()
) {
    val categorias by viewModel.categorias.collectAsState()
    val operacion by viewModel.operacion.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val contexto = LocalContext.current

    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var categoriaEditando by remember { mutableStateOf<Categoria?>(null) }
    var categoriaCambiandoEstado by remember { mutableStateOf<Categoria?>(null) }

    LaunchedEffect(operacion) {
        when (val actual = operacion) {
            is OperacionEstado.Exitosa -> {
                Toast.makeText(contexto, actual.mensaje, Toast.LENGTH_LONG).show()
                viewModel.operacionConsumida()
            }
            is OperacionEstado.Fallida -> {
                snackbarHostState.showSnackbar(actual.mensaje)
                viewModel.operacionConsumida()
            }
            else -> Unit
        }
    }

    val ocupado = operacion is OperacionEstado.EnProgreso

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoNueva = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva categoría")
            }
        }
    ) { padding ->
        if (categorias.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no hay categorías registradas. Toca el botón + para agregar la primera.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categorias, key = { it.id }) { categoria ->
                    FilaCategoria(
                        categoria = categoria,
                        ocupado = ocupado,
                        onEditar = { categoriaEditando = categoria },
                        onCambiarEstado = { categoriaCambiandoEstado = categoria }
                    )
                }
            }
        }
    }

    if (mostrarDialogoNueva) {
        DialogoFormularioCategoria(
            titulo = "Nueva categoría",
            nombreInicial = "",
            ocupado = ocupado,
            onConfirmar = { nombre ->
                mostrarDialogoNueva = false
                viewModel.registrar(nombre)
            },
            onCancelar = { mostrarDialogoNueva = false }
        )
    }

    categoriaEditando?.let { categoria ->
        DialogoFormularioCategoria(
            titulo = "Editar categoría",
            nombreInicial = categoria.nombre,
            ocupado = ocupado,
            onConfirmar = { nombre ->
                categoriaEditando = null
                viewModel.actualizar(categoria.id, nombre)
            },
            onCancelar = { categoriaEditando = null }
        )
    }

    categoriaCambiandoEstado?.let { categoria ->
        val activarla = !categoria.activa
        AlertDialog(
            onDismissRequest = { categoriaCambiandoEstado = null },
            title = { Text(if (activarla) "Activar categoría" else "Desactivar categoría") },
            text = {
                Text(
                    if (activarla) {
                        "¿Estás seguro de activar \"${categoria.nombre}\"? Volverá a estar disponible al registrar productos."
                    } else {
                        "¿Estás seguro de desactivar \"${categoria.nombre}\"? No se eliminará de la base de datos, solo dejará de estar disponible para nuevos productos."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    categoriaCambiandoEstado = null
                    viewModel.cambiarEstado(categoria.id, activarla)
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { categoriaCambiandoEstado = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FilaCategoria(
    categoria: Categoria,
    ocupado: Boolean,
    onEditar: () -> Unit,
    onCambiarEstado: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(categoria.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (categoria.activa) "Activa" else "Inactiva",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (categoria.activa) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                if (categoria.estadoSincronizacion.estaPendiente) {
                    Text(
                        "Pendiente de sincronización",
                        style = MaterialTheme.typography.labelSmall,
                        color = PanchitoAmber
                    )
                }
            }
            IconButton(onClick = onEditar, enabled = !ocupado) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar \"${categoria.nombre}\"")
            }
            Switch(
                checked = categoria.activa,
                onCheckedChange = { onCambiarEstado() },
                enabled = !ocupado
            )
        }
    }
}

@Composable
private fun DialogoFormularioCategoria(
    titulo: String,
    nombreInicial: String,
    ocupado: Boolean,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    // El error "obligatorio" solo se muestra despues de intentar guardar con el campo vacio, no
    // apenas se abre el dialogo. Desaparece solo en cuanto el usuario escribe algo.
    var intentoGuardar by remember { mutableStateOf(false) }
    val mostrarError = intentoGuardar && nombre.isBlank()

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la categoría") },
                singleLine = true,
                enabled = !ocupado,
                isError = mostrarError,
                supportingText = { if (mostrarError) Text("El nombre es obligatorio.") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nombre.isBlank()) intentoGuardar = true else onConfirmar(nombre)
                },
                enabled = !ocupado
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
