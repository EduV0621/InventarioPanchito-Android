package com.panchito.inventario.domain.model

import java.util.Date

enum class TipoMovimiento { ENTRADA, SALIDA }

enum class EstadoSincronizacion {
    SINCRONIZADO,

    PENDIENTE_CREAR,

    PENDIENTE_ACTUALIZAR,

    PENDIENTE_ELIMINAR;

    val estaPendiente: Boolean get() = this != SINCRONIZADO
}

data class Movimiento(
    val id: String = "",
    val productoId: String,
    val ownerUid: String,
    val tipo: TipoMovimiento,
    val cantidad: Int,
    val motivo: String,
    val fechaHora: Date = Date(),
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.PENDIENTE_CREAR,

    val updatedAt: Long = System.currentTimeMillis()
)

data class MovimientoDetalle(
    val movimiento: Movimiento,
    val productoNombre: String
)

const val MOTIVO_STOCK_INICIAL = "Stock inicial"

object MotivosMovimiento {
    val entrada = listOf(
        "Recepcion de mercaderia",
        "Compra",
        "Devolucion de cliente",
        "Ajuste de inventario",
        "Otro"
    )

    val salida = listOf(
        "Venta",
        "Producto daniado",
        "Producto vencido",
        "Merma",
        "Uso interno",
        "Devolucion a proveedor",
        "Ajuste de inventario",
        "Otro"
    )

    fun de(tipo: TipoMovimiento): List<String> =
        if (tipo == TipoMovimiento.ENTRADA) entrada else salida
}
