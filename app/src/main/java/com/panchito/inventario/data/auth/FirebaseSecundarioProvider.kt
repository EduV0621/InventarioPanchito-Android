package com.panchito.inventario.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

private const val NOMBRE_APP_SECUNDARIA = "SecundariaRegistroEmpleados"

object FirebaseSecundarioProvider {
    @Volatile private var authSecundario: FirebaseAuth? = null

    fun obtenerAuth(context: Context): FirebaseAuth {
        authSecundario?.let { return it }
        synchronized(this) {
            authSecundario?.let { return it }

            val appPorDefecto = FirebaseApp.getInstance()
            val opciones = FirebaseOptions.Builder()
                .setApiKey(appPorDefecto.options.apiKey)
                .setApplicationId(appPorDefecto.options.applicationId)
                .setProjectId(appPorDefecto.options.projectId)
                .build()

            val appSecundaria = try {
                FirebaseApp.getInstance(NOMBRE_APP_SECUNDARIA)
            } catch (e: IllegalStateException) {
                FirebaseApp.initializeApp(context.applicationContext, opciones, NOMBRE_APP_SECUNDARIA)
            }

            return FirebaseAuth.getInstance(appSecundaria).also { authSecundario = it }
        }
    }
}
