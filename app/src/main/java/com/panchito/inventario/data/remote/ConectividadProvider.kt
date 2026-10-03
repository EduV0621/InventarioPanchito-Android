package com.panchito.inventario.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

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
