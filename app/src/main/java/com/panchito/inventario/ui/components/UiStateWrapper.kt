package com.panchito.inventario.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun <T> UiStateWrapper(
    state: UiState<T>,
    modifier: Modifier = Modifier.fillMaxSize(),
    mensajeVacio: String = "No hay datos para mostrar todavia",
    contenidoExitoso: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> EstadoCentrado(modifier) {
            CircularProgressIndicator()
            Text("Cargando...", modifier = Modifier.padding(top = 12.dp))
        }
        is UiState.Success -> contenidoExitoso(state.data)
        is UiState.Error -> EstadoCentrado(modifier) {
            Text("Ocurrio un error", style = MaterialTheme.typography.titleMedium)
            Text(state.mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        is UiState.Empty -> EstadoCentrado(modifier) {
            Text(mensajeVacio, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        is UiState.Offline -> EstadoCentrado(modifier) {
            Text("Sin conexion", style = MaterialTheme.typography.titleMedium)
            Text("Trabajando con los datos guardados localmente", style = MaterialTheme.typography.bodyMedium)
        }
        is UiState.Syncing -> EstadoCentrado(modifier) {
            CircularProgressIndicator()
            Text("Sincronizando cambios...", modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun EstadoCentrado(modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { contenido() }
}
