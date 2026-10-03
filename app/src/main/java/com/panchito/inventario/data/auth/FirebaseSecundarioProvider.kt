package com.panchito.inventario.data.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth

private const val NOMBRE_APP_SECUNDARIA = "SecundariaRegistroEmpleados"

/**
 * Provee una instancia SECUNDARIA de FirebaseApp/FirebaseAuth, distinta de la instancia por
 * defecto que usa [FirebaseAuthDataSource] para el login del Administrador.
 *
 * Motivo (Fase 1, "Registro de empleado"): FirebaseAuth.createUserWithEmailAndPassword deja
 * iniciada la sesion de la cuenta NUEVA en la instancia donde se llama. Si se llamara sobre la
 * instancia por defecto, se cerraria la sesion del Administrador que esta registrando al
 * empleado. Usando una instancia secundaria (misma configuracion de Firebase, mismo proyecto),
 * la cuenta nueva se crea "a un lado" y la sesion del Administrador nunca se toca.
 *
 * Reutiliza las mismas credenciales (apiKey/applicationId/projectId) que ya trae
 * app/google-services.json para la instancia por defecto: no hace falta un proyecto de Firebase
 * distinto ni un segundo google-services.json.
 */
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
                // Todavia no existia: se crea una unica vez por proceso.
                FirebaseApp.initializeApp(context.applicationContext, opciones, NOMBRE_APP_SECUNDARIA)
            }

            return FirebaseAuth.getInstance(appSecundaria).also { authSecundario = it }
        }
    }
}
