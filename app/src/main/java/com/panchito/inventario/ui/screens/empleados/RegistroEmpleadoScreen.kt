package com.panchito.inventario.ui.screens.empleados

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.panchito.inventario.domain.model.Rol
import com.panchito.inventario.ui.components.UiState
import com.panchito.inventario.ui.registroEmpleadoViewModel

/**
 * Fase 1: el Administrador registra un empleado nuevo. Al confirmar:
 * 1) se crea la cuenta en Firebase Authentication (instancia secundaria, sin cerrar la sesion
 *    del Administrador, ver [com.panchito.inventario.data.auth.EmpleadoAuthDataSource]);
 * 2) se guarda en el backend (MySQL via empleados.php) con el rol elegido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroEmpleadoScreen(
    onVolver: () -> Unit,
    viewModel: RegistroEmpleadoViewModel = registroEmpleadoViewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var rolSeleccionado by remember { mutableStateOf(Rol.EMPLEADO) }
    var mostrarClave by remember { mutableStateOf(false) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    val estado by viewModel.estado.collectAsState()
    val guardando = estado is UiState.Loading

    // Al registrar con exito, se limpia el formulario para poder cargar otro empleado sin salir
    // de la pantalla (el Administrador puede registrar varios seguidos).
    LaunchedEffect(estado) {
        if (estado is UiState.Success) {
            nombre = ""; dni = ""; telefono = ""; correo = ""; clave = ""
            rolSeleccionado = Rol.EMPLEADO
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar empleado") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Solo el Administrador puede registrar, editar y desactivar empleados.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it.filter { c -> !c.isDigit() } },
                label = { Text("Nombre completo") },
                singleLine = true,
                enabled = !guardando,
                supportingText = { Text("No debe contener números.") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = dni,
                onValueChange = { if (it.length <= 8) dni = it.filter { c -> c.isDigit() } },
                label = { Text("DNI") },
                singleLine = true,
                enabled = !guardando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = telefono,
                onValueChange = { if (it.length <= 9) telefono = it.filter { c -> c.isDigit() } },
                label = { Text("Telefono") },
                singleLine = true,
                enabled = !guardando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo") },
                singleLine = true,
                enabled = !guardando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it },
                label = { Text("Contrasena") },
                singleLine = true,
                enabled = !guardando,
                visualTransformation = if (mostrarClave) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { mostrarClave = !mostrarClave }) {
                        Icon(
                            if (mostrarClave) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (mostrarClave) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                supportingText = { Text("Mínimo 8 caracteres, con mayúscula, minúscula, número y símbolo.") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text("Rol", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Rol.entries.forEach { rol ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = rolSeleccionado == rol,
                            onClick = { rolSeleccionado = rol },
                            enabled = !guardando
                        )
                        Text(if (rol == Rol.ADMINISTRADOR) "Administrador" else "Empleado")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { mostrarConfirmacion = true },
                enabled = !guardando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar empleado")
                }
            }

            when (val actual = estado) {
                is UiState.Error -> {
                    Spacer(Modifier.height(12.dp))
                    Text(actual.mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                is UiState.Success -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Empleado registrado correctamente.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> Unit
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Confirmar registro") },
            text = { Text("¿Estás seguro de registrar a \"$nombre\" como ${if (rolSeleccionado == Rol.ADMINISTRADOR) "Administrador" else "Empleado"}?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    viewModel.registrarEmpleado(
                        nombre = nombre,
                        dni = dni,
                        telefono = telefono,
                        correo = correo,
                        clave = clave,
                        rol = rolSeleccionado
                    )
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar") }
            }
        )
    }
}
