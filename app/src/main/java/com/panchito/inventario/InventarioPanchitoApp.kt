package com.panchito.inventario

import android.app.Application
import com.google.firebase.FirebaseApp
import com.panchito.inventario.data.sync.SincronizacionProgramador
import com.panchito.inventario.di.AppContainer

class InventarioPanchitoApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        SincronizacionProgramador.programarPeriodica(this)
        SincronizacionProgramador.solicitar(this)
    }
}
