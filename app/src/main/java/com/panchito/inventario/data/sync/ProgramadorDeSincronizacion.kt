package com.panchito.inventario.data.sync

/**
 * Permite que los repositorios pidan una sincronizacion en segundo plano sin conocer WorkManager
 * ni necesitar un Context. La implementacion real se arma en el AppContainer y delega en
 * [SincronizacionProgramador].
 */
fun interface ProgramadorDeSincronizacion {
    fun solicitar()
}
