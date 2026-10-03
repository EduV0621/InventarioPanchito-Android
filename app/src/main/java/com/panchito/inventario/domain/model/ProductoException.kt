package com.panchito.inventario.domain.model

/**
 * Errores de negocio del dominio de Productos. El Repository los devuelve dentro de un
 * Result.failure(...) y el ViewModel decide como mostrarlos en la UI.
 */
sealed class ProductoException(mensaje: String) : Exception(mensaje) {
    class CodigoDuplicado(codigo: String) :
        ProductoException("Ya existe un producto con el codigo \"$codigo\"")

    class NoEncontrado : ProductoException("El producto ya no existe")

    class DatosInvalidos(mensaje: String) : ProductoException(mensaje)

    class Restriccion : ProductoException("No se pudo completar la operacion: el codigo ya existe o la categoria no es valida")

    /**
     * Fase 5: no hay un usuario autenticado. El inventario real esta asociado al usuario
     * (users/{uid}/products en Firestore), asi que sin sesion no se permite gestionarlo.
     */
    class SinSesion : ProductoException("Debes iniciar sesión para gestionar el inventario.")
}
