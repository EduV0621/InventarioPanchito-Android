package com.panchito.inventario.ui.screens.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.panchito.inventario.data.local.datastore.UserPreferencesManager
import com.panchito.inventario.data.repository.AuthRepository
import com.panchito.inventario.data.repository.EmpleadoRepository
import com.panchito.inventario.domain.model.Rol
import com.panchito.inventario.ui.components.UiState
import com.panchito.inventario.ui.components.mensajeDeAutenticacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val empleadoRepository: EmpleadoRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val alIniciarSesion: () -> Unit = {}
) : ViewModel() {
    private val _estado = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val estado: StateFlow<UiState<Unit>> = _estado.asStateFlow()

    fun iniciarSesion(correo: String, clave: String) {
        val error = validarLogin(correo, clave)
        if (error != null) {
            _estado.value = UiState.Error(error)
            return
        }
        viewModelScope.launch {
            _estado.value = UiState.Loading
            authRepository.iniciarSesion(correo.trim(), clave)
                .onSuccess { usuario ->
                    val correoFinal = usuario.correo ?: correo.trim()
                    resolverYGuardarRol(usuario.uid, correoFinal)

                    empleadoRepository.sincronizarUidFirebaseSiFalta(correoFinal, usuario.uid)

                    alIniciarSesion()
                    _estado.value = UiState.Success(Unit)
                }
                .onFailure { _estado.value = UiState.Error(it.mensajeDeAutenticacion()) }
        }
    }

    private suspend fun resolverYGuardarRol(uid: String, correo: String) {
        val rol = empleadoRepository.resolverRol(correo).getOrDefault(Rol.EMPLEADO)
        userPreferencesManager.guardarSesion(uid, rol.name)
    }

    private fun validarLogin(correo: String, clave: String): String? = when {
        correo.isBlank() -> "El correo electrónico es obligatorio."
        !Patterns.EMAIL_ADDRESS.matcher(correo).matches() -> "El correo electrónico no tiene un formato válido."
        clave.isBlank() -> "La contraseña es obligatoria."
        else -> null
    }
}
