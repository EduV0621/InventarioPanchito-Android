package com.panchito.inventario.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Comprueba si el dispositivo tiene una conexion de red utilizable ANTES de intentar una
 * solicitud HTTP, para no ejecutar solicitudes innecesarias cuando no hay Internet
 * (Fase 3, seccion "Conectividad"). Usa el permiso ACCESS_NETWORK_STATE, ya declarado en el
 * AndroidManifest desde la Fase 2.
 */
class ConectividadProvider(context: Context) {

    private val appContext = context.applicationContext

    fun hayConexion(): Boolean {
        val gestor = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val redActiva = gestor.activeNetwork ?: return false
        val capacidades = gestor.getNetworkCapabilities(redActiva) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
