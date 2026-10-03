package com.panchito.inventario.di

import android.content.Context
import com.panchito.inventario.data.auth.AuthDataSource
import com.panchito.inventario.data.auth.EmpleadoAuthDataSource
import com.panchito.inventario.data.auth.FirebaseAuthDataSource
import com.panchito.inventario.data.local.AppDatabase
import com.panchito.inventario.data.local.datasource.CategoriaLocalDataSource
import com.panchito.inventario.data.local.datasource.EmpleadoLocalDataSource
import com.panchito.inventario.data.local.datasource.MovimientoLocalDataSource
import com.panchito.inventario.data.local.datasource.ProductoLocalDataSource
import com.panchito.inventario.data.local.datasource.RoomCategoriaLocalDataSource
import com.panchito.inventario.data.local.datasource.RoomEmpleadoLocalDataSource
import com.panchito.inventario.data.local.datasource.RoomMovimientoLocalDataSource
import com.panchito.inventario.data.local.datasource.RoomProductoLocalDataSource
import com.panchito.inventario.data.local.datastore.UserPreferencesManager
import com.panchito.inventario.data.remote.ConectividadProvider
import com.panchito.inventario.data.remote.RetrofitClient
import com.panchito.inventario.data.remote.datasource.CategoriaRemoteDataSource
import com.panchito.inventario.data.remote.datasource.EmpleadoRemoteDataSource
import com.panchito.inventario.data.remote.datasource.MovimientoRemoteDataSource
import com.panchito.inventario.data.remote.datasource.ProductoRemoteDataSource
import com.panchito.inventario.data.remote.datasource.RetrofitCategoriaRemoteDataSource
import com.panchito.inventario.data.remote.datasource.RetrofitEmpleadoRemoteDataSource
import com.panchito.inventario.data.remote.datasource.RetrofitMovimientoRemoteDataSource
import com.panchito.inventario.data.remote.datasource.RetrofitProductoRemoteDataSource
import com.panchito.inventario.data.repository.AuthRepository
import com.panchito.inventario.data.repository.AuthRepositoryImpl
import com.panchito.inventario.data.repository.CategoriaRepository
import com.panchito.inventario.data.repository.CategoriaRepositoryImpl
import com.panchito.inventario.data.repository.EmpleadoRepository
import com.panchito.inventario.data.repository.EmpleadoRepositoryImpl
import com.panchito.inventario.data.repository.MovimientoRepository
import com.panchito.inventario.data.repository.MovimientoRepositoryImpl
import com.panchito.inventario.data.repository.ProductoRepository
import com.panchito.inventario.data.repository.ProductoRepositoryImpl
import com.panchito.inventario.data.sync.ApiSyncManager
import com.panchito.inventario.data.sync.ProgramadorDeSincronizacion
import com.panchito.inventario.data.sync.SincronizacionProgramador

/**
 * Contenedor de dependencias manual (sin Hilt/Koin). Es el unico lugar donde se decide que
 * implementacion usa cada capa.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext) { authDataSource.usuarioActual()?.uid }
    }

    private val productoLocalDataSource: ProductoLocalDataSource by lazy {
        RoomProductoLocalDataSource(database.productoDao())
    }
    private val categoriaLocalDataSource: CategoriaLocalDataSource by lazy {
        RoomCategoriaLocalDataSource(database.categoriaDao())
    }
    private val movimientoLocalDataSource: MovimientoLocalDataSource by lazy {
        RoomMovimientoLocalDataSource(database.movimientoDao())
    }

    /** Fuente remota (Retrofit) de productos: tu API REST propia (PHP + MySQL en XAMPP). */
    private val conectividadProvider: ConectividadProvider by lazy { ConectividadProvider(appContext) }

    private val productoRemoteDataSource: ProductoRemoteDataSource by lazy {
        RetrofitProductoRemoteDataSource(RetrofitClient.productoApiService, conectividadProvider)
    }

    /** Fuente remota (Retrofit) de categorias (categorias.php). */
    private val categoriaRemoteDataSource: CategoriaRemoteDataSource by lazy {
        RetrofitCategoriaRemoteDataSource(RetrofitClient.categoriaApiService, conectividadProvider)
    }

    /** Fase 3: fuente remota (Retrofit) de movimientos (movimientos.php). */
    private val movimientoRemoteDataSource: MovimientoRemoteDataSource by lazy {
        RetrofitMovimientoRemoteDataSource(RetrofitClient.movimientoApiService, conectividadProvider)
    }

    /** Unico componente que sincroniza Room <-> tu API REST PHP. */
    val syncManager: ApiSyncManager by lazy {
        ApiSyncManager(
            categoriasLocal = categoriaLocalDataSource,
            categoriasRemoto = categoriaRemoteDataSource,
            productosLocal = productoLocalDataSource,
            productosRemoto = productoRemoteDataSource,
            movimientosLocal = movimientoLocalDataSource,
            movimientosRemoto = movimientoRemoteDataSource,
            auth = authDataSource,
            conectividad = conectividadProvider
        )
    }

    /** Los repositorios piden una sincronizacion de fondo sin conocer WorkManager. */
    private val programadorDeSincronizacion: ProgramadorDeSincronizacion =
        ProgramadorDeSincronizacion { SincronizacionProgramador.solicitar(appContext) }

    val productoRepository: ProductoRepository by lazy {
        ProductoRepositoryImpl(
            local = productoLocalDataSource,
            auth = authDataSource,
            movimientos = movimientoLocalDataSource,
            sincronizador = syncManager,
            programador = programadorDeSincronizacion
        )
    }

    val categoriaRepository: CategoriaRepository by lazy {
        CategoriaRepositoryImpl(
            local = categoriaLocalDataSource,
            sincronizador = syncManager,
            programador = programadorDeSincronizacion
        )
    }

    val movimientoRepository: MovimientoRepository by lazy {
        MovimientoRepositoryImpl(
            local = movimientoLocalDataSource,
            productos = productoLocalDataSource,
            auth = authDataSource,
            sincronizador = syncManager,
            programador = programadorDeSincronizacion
        )
    }

    /** Autenticacion con Firebase Authentication (Email/Password); se mantiene sin cambios. */
    private val authDataSource: AuthDataSource by lazy { FirebaseAuthDataSource() }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(authDataSource) }

    /** Fase 1: sesion local (rol cacheado) para decidir que ve cada usuario sin red. */
    val userPreferencesManager: UserPreferencesManager by lazy { UserPreferencesManager(appContext) }

    private val empleadoLocalDataSource: EmpleadoLocalDataSource by lazy {
        RoomEmpleadoLocalDataSource(database.empleadoDao())
    }

    private val empleadoRemoteDataSource: EmpleadoRemoteDataSource by lazy {
        RetrofitEmpleadoRemoteDataSource(RetrofitClient.empleadoApiService, conectividadProvider)
    }

    /** Crea cuentas de empleados en una instancia SECUNDARIA de Firebase (no toca la sesion del Administrador). */
    private val empleadoAuthDataSource: EmpleadoAuthDataSource by lazy { EmpleadoAuthDataSource(appContext) }

    val empleadoRepository: EmpleadoRepository by lazy {
        EmpleadoRepositoryImpl(
            remoto = empleadoRemoteDataSource,
            local = empleadoLocalDataSource,
            empleadoAuth = empleadoAuthDataSource,
            conectividad = conectividadProvider
        )
    }
}