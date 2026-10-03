package com.panchito.inventario.navigation

/** Nombre del argumento de navegacion con el id del producto. */
const val ARG_PRODUCTO_ID = "productoId"

/**
 * Valor del argumento cuando el formulario sirve para REGISTRAR un producto nuevo.
 * Desde la Fase 5.1 los ids son UUID (String), asi que ya no puede usarse el 0 como "producto nuevo".
 */
const val ID_PRODUCTO_NUEVO = "nuevo"

/**
 * Rutas de navegacion de la aplicacion.
 * Producto: detalle y formulario reciben el id del producto como argumento
 * ([ID_PRODUCTO_NUEVO] en el formulario = producto nuevo).
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    /** Inventario REAL del Minimarket (Room + copia en Cloud Firestore del usuario autenticado). */
    data object Catalogo : Screen("catalogo")
    data object Movimientos : Screen("movimientos")
    /** Fase 5.2: formulario de registro de una entrada/salida de inventario. */
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
