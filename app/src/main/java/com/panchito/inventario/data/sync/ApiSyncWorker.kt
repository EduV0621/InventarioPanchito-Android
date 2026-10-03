package com.panchito.inventario.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.panchito.inventario.InventarioPanchitoApp
import java.util.concurrent.TimeUnit

private const val TAG = "ApiSyncWorker"

private const val TRABAJO_INMEDIATO = "sincronizacion_inventario_inmediata"
private const val TRABAJO_PERIODICO = "sincronizacion_inventario_periodica"
private const val MAXIMO_REINTENTOS = 5

class ApiSyncWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val contenedor = (applicationContext as InventarioPanchitoApp).container
        return when (val resultado = contenedor.syncManager.sincronizarAhora(incluirDescarga = true)) {
            ResultadoSincronizacion.COMPLETADA -> Result.success()
            ResultadoSincronizacion.SIN_SESION -> Result.success()
            ResultadoSincronizacion.SIN_CONEXION,
            ResultadoSincronizacion.ERROR -> {
                Log.w(TAG, "Sincronizacion incompleta ($resultado), intento ${runAttemptCount + 1}")
                if (runAttemptCount >= MAXIMO_REINTENTOS) Result.failure() else Result.retry()
            }
        }
    }
}

object SincronizacionProgramador {
    private val soloConRed = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun solicitar(contexto: Context) {
        val solicitud = OneTimeWorkRequestBuilder<ApiSyncWorker>()
            .setConstraints(soloConRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniqueWork(TRABAJO_INMEDIATO, ExistingWorkPolicy.KEEP, solicitud)
    }

    fun programarPeriodica(contexto: Context) {
        val solicitud = PeriodicWorkRequestBuilder<ApiSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(soloConRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, solicitud)
    }
}
