package com.panchito.inventario

import android.app.Application
import com.google.firebase.FirebaseApp
import com.panchito.inventario.data.sync.SincronizacionProgramador
import com.panchito.inventario.di.AppContainer

/**
 * Clase Application del proyecto. Crea el contenedor de dependencias que usan los ViewModel.
 */
class InventarioPanchitoApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Fase 4: Firebase Authentication. El SDK ya se auto-inicializa via su ContentProvider
        // (FirebaseInitProvider) leyendo google-services.json; esta llamada es explicita e
        // idempotente, y sirve de resguardo si esa inicializacion automatica falla.
        FirebaseApp.initializeApp(this)
        // Fase 6: red de seguridad de la sincronizacion. WorkManager la ejecutara cuando haya red,
        // sin depender de que el usuario abra una pantalla concreta. Es un trabajo unico (KEEP),
        // asi que llamarlo en cada arranque no crea trabajos duplicados.
        SincronizacionProgramador.programarPeriodica(this)
        SincronizacionProgramador.solicitar(this)
    }
}
