package com.panchito.inventario.domain.model

/**
 * Errores de negocio del dominio de Categorias. Mismo patron que [ProductoException]:
 * el Repository los devuelve dentro de un Result.failure(...) y el ViewModel decide como
 * mostrarlos en la UI.
 */
sealed class CategoriaException(mensaje: String) : Exception(mensaje) {
    class NombreVacio : CategoriaException("El nombre de la categoría es obligatorio.")

    class NombreDuplicado(nombre: String) :
        CategoriaException("Ya existe una categoría con el nombre \"$nombre\".")

    class NoEncontrada : CategoriaException("La categoría ya no existe.")
}
