package com.panchito.inventario.domain.model

sealed class ProductoException(mensaje: String) : Exception(mensaje) {
    class CodigoDuplicado(codigo: String) :
        ProductoException("Ya existe un producto con el codigo \"$codigo\"")

    class NoEncontrado : ProductoException("El producto ya no existe")

    class DatosInvalidos(mensaje: String) : ProductoException(mensaje)

    class Restriccion : ProductoException("No se pudo completar la operacion: el codigo ya existe o la categoria no es valida")

    class SinSesion : ProductoException("Debes iniciar sesión para gestionar el inventario.")
}
