package com.panchito.inventario.domain.model

sealed class CategoriaException(mensaje: String) : Exception(mensaje) {
    class NombreVacio : CategoriaException("El nombre de la categoría es obligatorio.")

    class NombreDuplicado(nombre: String) :
        CategoriaException("Ya existe una categoría con el nombre \"$nombre\".")

    class NoEncontrada : CategoriaException("La categoría ya no existe.")
}
