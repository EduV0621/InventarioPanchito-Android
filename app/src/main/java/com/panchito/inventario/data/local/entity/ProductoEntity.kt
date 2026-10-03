package com.panchito.inventario.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room de Producto (persistencia local, funcionamiento offline).
 *
 * Fase 5.1:
 * - [id] es un identificador ESTABLE de tipo String (UUID) generado al registrar el producto.
 *   Es el mismo id que se usa como nombre del documento en Firestore
 *   (users/{ownerUid}/products/{id}), para que una futura sincronizacion no tenga que traducir ids.
 * - [ownerUid] es el UID de Firebase Authentication del usuario duenio del producto. La base local
 *   es compartida por todos los usuarios del dispositivo, asi que TODA consulta filtra por este
 *   campo: un usuario nunca ve ni modifica los productos de otro.
 * El codigo interno es unico POR USUARIO (indice unico ownerUid + codigo), no a nivel de dispositivo.
 */
@Entity(
    tableName = "productos",
    indices = [
        // Fase 6: el indice deja de ser UNIQUE porque un producto eliminado logicamente conserva su
        // fila (y su codigo) como lapida para no romper la relacion con los movimientos historicos,
        // y porque una descarga desde Firestore no debe fallar por una colision de codigos. La regla
        // "no hay dos productos con el mismo codigo" sigue vigente y se valida en el Repository.
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
    // --- Fase 6: campos de sincronizacion con Cloud Firestore ---
    val estadoSincronizacion: String = "SINCRONIZADO",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
