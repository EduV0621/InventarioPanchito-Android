package com.panchito.inventario.ui.screens.movimientos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.panchito.inventario.domain.model.EstadoSincronizacion
import com.panchito.inventario.domain.model.MovimientoDetalle
import com.panchito.inventario.domain.model.TipoMovimiento
import com.panchito.inventario.ui.components.UiStateWrapper
import com.panchito.inventario.ui.components.comoFecha
import com.panchito.inventario.ui.movimientosViewModel
import com.panchito.inventario.ui.theme.PanchitoGreen
import com.panchito.inventario.ui.theme.PanchitoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosScreen(
    onVolver: () -> Unit,
    onRegistrarMovimiento: () -> Unit,
    viewModel: MovimientosViewModel = movimientosViewModel()
) {
    val estado by viewModel.estado.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movimientos de inventario") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRegistrarMovimiento,
                icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                text = { Text("Registrar") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            UiStateWrapper(
                state = estado,
                mensajeVacio = "Todavía no hay movimientos registrados"
            ) { movimientos ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movimientos) { detalle -> FilaMovimiento(detalle) }
                }
            }
        }
    }
}

@Composable
private fun FilaMovimiento(detalle: MovimientoDetalle) {
    val movimiento = detalle.movimiento
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val esEntrada = movimiento.tipo == TipoMovimiento.ENTRADA
            Text(
                text = if (esEntrada) "+${movimiento.cantidad}" else "-${movimiento.cantidad}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (esEntrada) PanchitoGreen else PanchitoRed
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (esEntrada) "ENTRADA" else "SALIDA",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (esEntrada) PanchitoGreen else PanchitoRed
                )
                Text(detalle.productoNombre, style = MaterialTheme.typography.titleMedium)
                Text(movimiento.motivo, style = MaterialTheme.typography.bodyMedium)
                Text(movimiento.fechaHora.comoFecha(), style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                imageVector = if (movimiento.estadoSincronizacion == EstadoSincronizacion.SINCRONIZADO)
                    Icons.Filled.CloudDone else Icons.Filled.CloudQueue,
                contentDescription = movimiento.estadoSincronizacion.name
            )
        }
    }
}
