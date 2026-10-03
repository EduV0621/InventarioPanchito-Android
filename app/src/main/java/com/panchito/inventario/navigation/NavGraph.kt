package com.panchito.inventario.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.panchito.inventario.domain.model.Rol
import com.panchito.inventario.ui.appContainer
import com.panchito.inventario.ui.screens.catalogo.CatalogoScreen
import com.panchito.inventario.ui.screens.categorias.CategoriasScreen
import com.panchito.inventario.ui.screens.dashboard.DashboardScreen
import com.panchito.inventario.ui.screens.empleados.RegistroEmpleadoScreen
import com.panchito.inventario.ui.screens.login.LoginScreen
import com.panchito.inventario.ui.screens.movimientos.MovimientoFormScreen
import com.panchito.inventario.ui.screens.movimientos.MovimientosScreen
import com.panchito.inventario.ui.screens.productos.ProductoDetalleScreen
import com.panchito.inventario.ui.screens.productos.ProductoFormScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val contenedor = appContainer()
    val authRepository = contenedor.authRepository
    val destinoInicial =
        if (authRepository.usuarioActual() != null) Screen.Dashboard.route else Screen.Login.route

    val rolActual by contenedor.userPreferencesManager.rolActual.collectAsState(initial = null)
    val esAdministrador = rolActual == Rol.ADMINISTRADOR.name
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginExitoso = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                esAdministrador = esAdministrador,
                onIrACatalogo = { navController.navigate(Screen.Catalogo.route) },
                onIrAMovimientos = { navController.navigate(Screen.Movimientos.route) },
                onIrARegistroEmpleado = { navController.navigate(Screen.RegistroEmpleado.route) },
                onIrACategorias = { navController.navigate(Screen.Categorias.route) },
                onCerrarSesion = {
                    authRepository.cerrarSesion()
                    scope.launch { contenedor.userPreferencesManager.cerrarSesion() }
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Screen.RegistroEmpleado.route) {
            LaunchedEffect(rolActual) {
                if (rolActual != null && !esAdministrador) {
                    navController.popBackStack()
                }
            }
            RegistroEmpleadoScreen(onVolver = { navController.popBackStack() })
        }

        composable(Screen.Categorias.route) {
            LaunchedEffect(rolActual) {
                if (rolActual != null && !esAdministrador) {
                    navController.popBackStack()
                }
            }
            CategoriasScreen(onVolver = { navController.popBackStack() })
        }

        composable(Screen.Catalogo.route) {
            CatalogoScreen(
                esAdministrador = esAdministrador,
                onVolver = { navController.popBackStack() },
                onNuevoProducto = { navController.navigate(Screen.ProductoForm.crearRuta()) },
                onVerProducto = { id -> navController.navigate(Screen.ProductoDetalle.crearRuta(id)) }
            )
        }

        composable(
            route = Screen.ProductoDetalle.route,
            arguments = listOf(navArgument(ARG_PRODUCTO_ID) { type = NavType.StringType })
        ) { entrada ->
            val productoId = entrada.arguments?.getString(ARG_PRODUCTO_ID).orEmpty()
            ProductoDetalleScreen(
                productoId = productoId,
                onVolver = { navController.popBackStack() },
                onEditar = { navController.navigate(Screen.ProductoForm.crearRuta(productoId)) }
            )
        }

        composable(
            route = Screen.ProductoForm.route,
            arguments = listOf(navArgument(ARG_PRODUCTO_ID) { type = NavType.StringType })
        ) { entrada ->
            val productoId = entrada.arguments?.getString(ARG_PRODUCTO_ID) ?: ID_PRODUCTO_NUEVO
            ProductoFormScreen(
                productoId = productoId,
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Screen.Movimientos.route) {
            MovimientosScreen(
                onVolver = { navController.popBackStack() },
                onRegistrarMovimiento = { navController.navigate(Screen.MovimientoForm.route) }
            )
        }

        composable(Screen.MovimientoForm.route) {
            MovimientoFormScreen(onVolver = { navController.popBackStack() })
        }
    }
}
