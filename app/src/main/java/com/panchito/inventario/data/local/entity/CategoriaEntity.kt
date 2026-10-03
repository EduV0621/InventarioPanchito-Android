package com.panchito.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val activa: Boolean = true,
    val estadoSincronizacion: String = "SINCRONIZADO",
    val updatedAt: Long = 0L
)
