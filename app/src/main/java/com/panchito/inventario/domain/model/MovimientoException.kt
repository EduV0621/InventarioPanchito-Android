package com.panchito.inventario.domain.model

sealed class MovimientoException(mensaje: String) : Exception(mensaje) {
    class SinSesion : MovimientoException("Debes iniciar sesión para registrar movimientos.")

    class ProductoNoSeleccionado : MovimientoException("Selecciona un producto.")

    class ProductoNoEncontrado : MovimientoException("El producto seleccionado ya no está disponible.")

    class CantidadInvalida : MovimientoException("La cantidad debe ser un número mayor que 0.")

    class MotivoInvalido : MovimientoException("Selecciona un motivo.")

    class StockInsuficiente(disponible: Int) :
        MovimientoException("Stock insuficiente. Stock disponible: $disponible.")
}
