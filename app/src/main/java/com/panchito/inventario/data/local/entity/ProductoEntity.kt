package com.panchito.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    indices = [

        Index(value = ["ownerUid", "codigo"]),
        Index(value = ["categoriaId"]),
        Index(value = ["ownerUid"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"]
        )
    ]
)
data class ProductoEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val codigo: String,
    val nombre: String,
    val categoriaId: Long,
    val precio: Double,
    val stock: Int,
    val stockMinimo: Int,
    val fechaVencimientoMillis: Long? = null,
    val activo: Boolean = true,

    val estadoSincronizacion: String = "SINCRONIZADO",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
