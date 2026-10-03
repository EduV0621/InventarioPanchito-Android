package com.panchito.inventario.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.panchito.inventario.ui.components.UiStateWrapper
import com.panchito.inventario.ui.dashboardViewModel
import com.panchito.inventario.ui.theme.PanchitoAmber
import com.panchito.inventario.ui.theme.PanchitoRed

/**
 * Dashboard: resumen de alertas (stock bajo / agotados) calculado con los productos guardados
 * localmente, y accesos a Catalogo y Movimientos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    esAdministrador: Boolean,
    onIrACatalogo: () -> Unit,
    onIrAMovimientos: () -> Unit,
    onIrARegistroEmpleado: () -> Unit,
    onIrACategorias: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: DashboardViewModel = dashboardViewModel()
) {
    val estado by viewModel.estado.collectAsState()
    var mostrarConfirmacionSalir by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel - Inventario Panchito") },
                actions = {
                    IconButton(onClick = { mostrarConfirmacionSalir = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesion")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            Text(
                "Gestion responsable del inventario: controlar el stock y los vencimientos ayuda a reducir perdidas y desperdicio de productos.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))

            UiStateWrapper(state = estado, modifier = Modifier.fillMaxWidth()) { resumen ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TarjetaResumen(
                        icono = Icons.Filled.Inventory,
                        titulo = "Productos",
                        valor = resumen.totalProductos.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaResumen(
                        icono = Icons.Filled.Warning,
                        titulo = "Stock bajo",
                        valor = resumen.stockBajo.toString(),
                        color = PanchitoAmber,
                        modifier = Modifier.weight(1f)
                    )
                    TarjetaResumen(
                        icono = Icons.Filled.Warning,
                        titulo = "Agotados",
                        valor = resumen.agotados.toString(),
                        color = PanchitoRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Accesos rapidos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedButton(onClick = onIrACatalogo, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.ListAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ver catalogo de productos")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onIrAMovimientos, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ver movimientos de inventario")
            }
            // Fase 1 (Roles): solo el Administrador puede registrar empleados.
            if (esAdministrador) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onIrARegistroEmpleado, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar empleado (Administrador)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onIrACategorias, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Category, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gestionar categorías (Administrador)")
                }
            }
        }
    }

    if (mostrarConfirmacionSalir) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionSalir = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Estás seguro de que deseas salir de tu cuenta?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionSalir = false
                    onCerrarSesion()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionSalir = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TarjetaResumen(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(icono, contentDescription = titulo, tint = color)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.headlineSmall)
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
