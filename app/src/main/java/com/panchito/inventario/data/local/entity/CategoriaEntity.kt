package com.panchito.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room de Categoria.
 *
 * Version 5 de la base: se agregan [estadoSincronizacion] y [updatedAt], con el mismo significado
 * que en ProductoEntity. Room es la fuente de verdad; estos campos solo indican que falta subir
 * al servidor (API PHP + MySQL). La regla "no se permite crear dos categorias con el mismo
 * nombre" se valida en el Repository.
 */
@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val activa: Boolean = true,
    val estadoSincronizacion: String = "SINCRONIZADO",
    val updatedAt: Long = 0L
)
