package com.panchito.inventario.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.panchito.inventario.InventarioPanchitoApp
import com.panchito.inventario.di.AppContainer
import com.panchito.inventario.navigation.ID_PRODUCTO_NUEVO
import com.panchito.inventario.ui.screens.auth.AuthViewModel
import com.panchito.inventario.ui.screens.catalogo.CatalogoViewModel
import com.panchito.inventario.ui.screens.categorias.CategoriasViewModel
import com.panchito.inventario.ui.screens.dashboard.DashboardViewModel
import com.panchito.inventario.ui.screens.movimientos.MovimientoFormViewModel
import com.panchito.inventario.ui.screens.movimientos.MovimientosViewModel
import com.panchito.inventario.ui.screens.empleados.RegistroEmpleadoViewModel
import com.panchito.inventario.ui.screens.productos.ProductoDetalleViewModel
import com.panchito.inventario.ui.screens.productos.ProductoFormViewModel

/**
 * Fabricas de ViewModel: los ViewModel reciben sus repositorios por constructor (desde el AppContainer)
 * en lugar de crearlos ellos mismos, asi pueden cambiar de fuente de datos sin modificarse.
 *
 * No es privada porque NavGraph (Fase 4) tambien necesita el AppContainer para leer
 * authRepository.usuarioActual() y decidir el destino inicial (Login o Dashboard).
 */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as InventarioPanchitoApp).container

private fun <T : ViewModel> fabricaDe(crear: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = crear() as VM
    }

@Composable
fun catalogoViewModel(): CatalogoViewModel {
    val contenedor = appContainer()
    return viewModel(factory = fabricaDe { CatalogoViewModel(contenedor.productoRepository, contenedor.categoriaRepository) })
}

@Composable
fun dashboardViewModel(): DashboardViewModel {
    val contenedor = appContainer()
    return viewModel(factory = fabricaDe { DashboardViewModel(contenedor.productoRepository) })
}

/** productoId = [ID_PRODUCTO_NUEVO] (o vacio) significa "producto nuevo". */
@Composable
fun productoFormViewModel(productoId: String): ProductoFormViewModel {
    val contenedor = appContainer()
    return viewModel(
        factory = fabricaDe {
            ProductoFormViewModel(
                productoId = productoId.takeIf { it.isNotBlank() && it != ID_PRODUCTO_NUEVO },
                productoRepository = contenedor.productoRepository,
                categoriaRepository = contenedor.categoriaRepository
            )
        }
    )
}

/** Fase 5.2: historial de movimientos de inventario del usuario autenticado. */
@Composable
fun movimientosViewModel(): MovimientosViewModel {
    val contenedor = appContainer()
    return viewModel(factory = fabricaDe { MovimientosViewModel(contenedor.movimientoRepository) })
}

/** Fase 5.2: formulario de registro de entradas/salidas. */
@Composable
fun movimientoFormViewModel(): MovimientoFormViewModel {
    val contenedor = appContainer()
    return viewModel(
        factory = fabricaDe {
            MovimientoFormViewModel(contenedor.movimientoRepository, contenedor.productoRepository)
        }
    )
}

@Composable
fun productoDetalleViewModel(productoId: String): ProductoDetalleViewModel {
    val contenedor = appContainer()
    return viewModel(
        factory = fabricaDe {
            ProductoDetalleViewModel(productoId, contenedor.productoRepository, contenedor.categoriaRepository)
        }
    )
}

/** Fase 4: autenticacion con Firebase (Login). Fase 1: resuelve y cachea el rol tras el login. */
@Composable
fun authViewModel(): AuthViewModel {
    val contenedor = appContainer()
    return viewModel(
        factory = fabricaDe {
            AuthViewModel(contenedor.authRepository, contenedor.empleadoRepository, contenedor.userPreferencesManager)
        }
    )
}

/** Fase 1: registro de empleados (solo Administrador). */
@Composable
fun registroEmpleadoViewModel(): RegistroEmpleadoViewModel {
    val contenedor = appContainer()
    return viewModel(factory = fabricaDe { RegistroEmpleadoViewModel(contenedor.empleadoRepository) })
}

/** Gestion de categorias de producto (solo Administrador): registrar, editar y activar/desactivar. */
@Composable
fun categoriasViewModel(): CategoriasViewModel {
    val contenedor = appContainer()
    return viewModel(factory = fabricaDe { CategoriasViewModel(contenedor.categoriaRepository) })
}
