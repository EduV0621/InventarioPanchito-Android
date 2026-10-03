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

/**
 * ViewModel de autenticacion (Fase 4). Unico punto de acceso de la UI a Firebase Authentication,
 * siempre a traves de [AuthRepository] (arquitectura MVVM: UI -> ViewModel -> Repository ->
 * DataSource -> Firebase).
 *
 * Fase 1 (Roles): despues de un login exitoso, resuelve el ROL del usuario (ADMINISTRADOR |
 * EMPLEADO) contra el backend a traves de [EmpleadoRepository] y lo cachea en
 * [UserPreferencesManager], que es lo que despues lee el Dashboard/NavGraph para mostrar u
 * ocultar las opciones segun el rol. Si no se puede resolver el rol (p. ej. sin conexion y sin
 * cache local), el login igual se considera exitoso pero sin privilegios de Administrador, para
 * no dejar a nadie fuera de la app por un problema del backend.
 *
 * Reutiliza el [UiState] que ya usan Dashboard/Catalogo en vez de crear una jerarquia de estados
 * nueva: Loading = autenticando, Success = autenticado (y con el rol ya resuelto/cacheado),
 * Error = credenciales/validacion/red, Empty = formulario inicial, sin nada que mostrar todavia.
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val empleadoRepository: EmpleadoRepository,
    private val userPreferencesManager: UserPreferencesManager
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
                    // Fase 1 (fix del UID del admin): best-effort, no bloquea ni afecta el
                    // resultado del login aunque falle (ver EmpleadoRepository).
                    empleadoRepository.sincronizarUidFirebaseSiFalta(correoFinal, usuario.uid)
                    _estado.value = UiState.Success(Unit)
                }
                .onFailure { _estado.value = UiState.Error(it.mensajeDeAutenticacion()) }
        }
    }

    /**
     * Consulta el rol en el backend (o en la cache local si no hay conexion) y lo guarda en
     * DataStore. Un fallo aca NO bloquea el login: se guarda EMPLEADO como rol minimo por
     * defecto (menor privilegio posible) para que el usuario pueda seguir usando la app.
     */
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
