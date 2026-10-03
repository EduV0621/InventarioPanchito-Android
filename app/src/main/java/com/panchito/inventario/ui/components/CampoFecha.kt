package com.panchito.inventario.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.util.Date

/**
 * Campo de fecha reutilizable (fecha de vencimiento en Producto al editar, y en el formulario de
 * Movimientos al registrar una entrada/salida). Siempre es opcional: se puede dejar vacio o
 * limpiar con el icono de "Quitar fecha".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    fecha: Date?,
    onCambio: (Date?) -> Unit,
    etiqueta: String = "Fecha de vencimiento (opcional)"
) {
    var mostrarSelector by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = fecha?.comoFecha() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(etiqueta) },
        trailingIcon = {
            Row {
                if (fecha != null) {
                    IconButton(onClick = { onCambio(null) }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Quitar fecha")
                    }
                }
                IconButton(onClick = { mostrarSelector = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Elegir fecha")
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (mostrarSelector) {
        val estadoSelector = rememberDatePickerState(initialSelectedDateMillis = fecha?.aMillisUtcDelDia())
        DatePickerDialog(
            onDismissRequest = { mostrarSelector = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoSelector.selectedDateMillis?.let { onCambio(fechaLocalDesdeMillisUtc(it)) }
                    mostrarSelector = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelector = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoSelector)
        }
    }
}
