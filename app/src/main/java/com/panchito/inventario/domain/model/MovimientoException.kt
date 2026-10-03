package com.panchito.inventario.domain.model

/**
 * Errores de negocio del dominio de Movimientos. Siguen el mismo patron que [ProductoException]:
 * el Repository los devuelve dentro de un Result.failure(...) y el ViewModel decide como mostrarlos.
 * Todos llevan un mensaje ya apto para el usuario (sin detalles tecnicos).
 */
sealed class MovimientoException(mensaje: String) : Exception(mensaje) {

    /** No hay sesion de Firebase Authentication: el inventario pertenece al usuario autenticado. */
    class SinSesion : MovimientoException("Debes iniciar sesión para registrar movimientos.")

    class ProductoNoSeleccionado : MovimientoException("Selecciona un producto.")

    /** El producto no existe o no pertenece al usuario autenticado. */
    class ProductoNoEncontrado : MovimientoException("El producto seleccionado ya no está disponible.")

    class CantidadInvalida : MovimientoException("La cantidad debe ser un número mayor que 0.")

    class MotivoInvalido : MovimientoException("Selecciona un motivo.")

    class StockInsuficiente(disponible: Int) :
        MovimientoException("Stock insuficiente. Stock disponible: $disponible.")
}
