package com.panchito.inventario.navigation

const val ARG_PRODUCTO_ID = "productoId"

const val ID_PRODUCTO_NUEVO = "nuevo"

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")

    data object Catalogo : Screen("catalogo")
    data object Movimientos : Screen("movimientos")

    data object MovimientoForm : Screen("movimiento_form")
    data object RegistroEmpleado : Screen("registro_empleado")
    data object Categorias : Screen("categorias")

    data object ProductoDetalle : Screen("producto_detalle/{$ARG_PRODUCTO_ID}") {
        fun crearRuta(productoId: String) = "producto_detalle/$productoId"
    }

    data object ProductoForm : Screen("producto_form/{$ARG_PRODUCTO_ID}") {
        fun crearRuta(productoId: String = ID_PRODUCTO_NUEVO) = "producto_form/$productoId"
    }
}
