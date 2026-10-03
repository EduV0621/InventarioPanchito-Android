package com.panchito.inventario.ui.screens.empleados

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.repository.EmpleadoRepository
import com.panchito.inventario.domain.model.Rol
import com.panchito.inventario.ui.components.MENSAJE_SIN_CONEXION_REGISTRO_EMPLEADO
import com.panchito.inventario.ui.components.UiState
import com.panchito.inventario.ui.components.mensajeDeRegistroEmpleado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistroEmpleadoViewModel(
    private val empleadoRepository: EmpleadoRepository
) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val estado: StateFlow<UiState<Unit>> = _estado.asStateFlow()

    fun registrarEmpleado(
        nombre: String,
        dni: String,
        telefono: String,
        correo: String,
        clave: String,
        rol: Rol
    ) {
        val error = validar(nombre, dni, telefono, correo, clave)
        if (error != null) {
            _estado.value = UiState.Error(error)
            return
        }

        if (!empleadoRepository.hayConexion()) {
            _estado.value = UiState.Error(MENSAJE_SIN_CONEXION_REGISTRO_EMPLEADO)
            return
        }
        viewModelScope.launch {
            _estado.value = UiState.Loading

            if (empleadoRepository.existeDni(dni)) {
                _estado.value = UiState.Error("Ya existe un empleado registrado con ese DNI.")
                return@launch
            }
            empleadoRepository.registrarEmpleado(nombre, dni, telefono, correo, clave, rol)
                .onSuccess { _estado.value = UiState.Success(Unit) }
                .onFailure {
                    Log.w("RegistroEmpleado", "No se pudo registrar el empleado", it)
                    _estado.value = UiState.Error(it.mensajeDeRegistroEmpleado())
                }
        }
    }

    fun reiniciarEstado() {
        _estado.value = UiState.Empty
    }

    private fun validar(nombre: String, dni: String, telefono: String, correo: String, clave: String): String? = when {
        nombre.isBlank() -> "El nombre es obligatorio."
        nombre.any { it.isDigit() } -> "El nombre completo no debe contener números."
        dni.length != 8 -> "El DNI debe tener exactamente 8 dígitos."
        !dni.all { it.isDigit() } -> "El DNI solo debe contener números."
        telefono.length != 9 -> "El teléfono debe tener exactamente 9 dígitos."
        !telefono.all { it.isDigit() } -> "El teléfono solo debe contener números."
        correo.isBlank() -> "El correo es obligatorio."
        !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "El correo electrónico no tiene un formato válido."
        clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
        !clave.any { it.isUpperCase() } -> "La contraseña debe incluir al menos una letra mayúscula."
        !clave.any { it.isLowerCase() } -> "La contraseña debe incluir al menos una letra minúscula."
        !clave.any { it.isDigit() } -> "La contraseña debe incluir al menos un número."
        clave.none { !it.isLetterOrDigit() } -> "La contraseña debe incluir al menos un símbolo (por ejemplo: @, #, $, %)."
        else -> null
    }
}
