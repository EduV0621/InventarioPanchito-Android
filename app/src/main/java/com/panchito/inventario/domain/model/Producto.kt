package com.panchito.inventario.domain.model

import java.util.Date

/**
 * Modelo de dominio de Producto (capa Domain, independiente de Room/Firebase).
 * Reglas de negocio asociadas (ver informe, seccion "Reglas de negocio"):
 * - No se permite crear dos productos con el mismo codigo.
 * - stock y precio deben ser >= 0.
 * - Se desactiva logicamente en lugar de eliminarse si tiene movimientos asociados.
 */
data class Producto(
    /**
     * Identificador ESTABLE y unico del producto (UUID generado al registrarlo, Fase 5.1).
     * Es el mismo id en Room y en Firestore (users/{uid}/products/{id}) y no cambia al editar.
     * Cadena vacia = producto todavia no registrado.
     */
    val id: String = "",
    val codigo: String,
    val nombre: String,
    val categoriaId: Long,
    val precio: Double,
    val stock: Int,
    val stockMinimo: Int,
    val fechaVencimiento: Date? = null,
    val activo: Boolean = true,
    /** Fase 6: estado frente a Firestore. Room siempre manda; esto solo dice que falta subir. */
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
    /** Fase 6: ultima modificacion local (epoch millis). Politica de conflictos: gana el mas reciente. */
    val updatedAt: Long = System.currentTimeMillis(),
    /** Fase 6: borrado logico. No nulo = eliminado; no se muestra en el catalogo. */
    val deletedAt: Long? = null
) {
    /** stock <= stockMinimo (regla de negocio: alerta "stock bajo") */
    val stockBajo: Boolean get() = stock in 1..stockMinimo

    /** stock == 0 (regla de negocio: alerta "agotado") */
    val agotado: Boolean get() = stock == 0
}
