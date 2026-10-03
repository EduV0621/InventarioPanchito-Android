package com.panchito.inventario.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesManager(private val context: Context) {
    private object Keys {
        val USUARIO_ID = stringPreferencesKey("usuario_id")
        val USUARIO_ROL = stringPreferencesKey("usuario_rol")
    }

    val rolActual: Flow<String?> = context.dataStore.data.map { it[Keys.USUARIO_ROL] }

    suspend fun guardarSesion(usuarioId: String, rol: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USUARIO_ID] = usuarioId
            prefs[Keys.USUARIO_ROL] = rol
        }
    }

    suspend fun cerrarSesion() {
        context.dataStore.edit { it.clear() }
    }
}
