package com.panchito.inventario.domain.model

import java.util.Date

enum class TipoMovimiento { ENTRADA, SALIDA }

/**
 * Estado de sincronizacion de un registro local frente a Cloud Firestore (Fase 6).
 * Room es la fuente de verdad inmediata; este estado indica que le falta por subir a la nube.
 */
enum class EstadoSincronizacion {
    /** Ya reflejado en Firestore: no hay nada pendiente que enviar. */
    SINCRONIZADO,
    /** Creado localmente y todavia no existe en Firestore. */
    PENDIENTE_CREAR,
    /** Existe en Firestore pero con datos mas viejos que los locales. */
    PENDIENTE_ACTUALIZAR,
    /** Eliminado localmente (borrado logico); falta eliminar el documento remoto. */
    PENDIENTE_ELIMINAR;

    val estaPendiente: Boolean get() = this != SINCRONIZADO
}

/**
 * Modelo de dominio de Movimiento de inventario (entradas y salidas de stock).
 * Toda operacion registra usuario y fecha/hora exactas (Reglas de negocio del informe).
 *
 * Fase 5.2:
 * - [id] es un identificador estable (UUID), igual que el de Producto, para que una futura fase de
 *   sincronizacion pueda usar el mismo id en Room y en la nube.
 * - [productoId] referencia al producto por su ID ESTABLE (nunca por su nombre).
 * - [ownerUid] es el UID de Firebase Authentication del usuario duenio del movimiento: cada usuario
 *   ve unicamente su propio historial.
 * - [estadoSincronizacion] queda en PENDIENTE: la sincronizacion con Firestore NO se implementa en
 *   esta fase, el campo solo deja el terreno preparado.
 */
data class Movimiento(
    val id: String = "",
    val productoId: String,
    val ownerUid: String,
    val tipo: TipoMovimiento,
    val cantidad: Int,
    val motivo: String,
    val fechaHora: Date = Date(),
    val estadoSincronizacion: EstadoSincronizacion = EstadoSincronizacion.PENDIENTE_CREAR,
    /** Marca de tiempo del ultimo cambio local; es la base de la politica de conflictos. */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Movimiento acompaniado del nombre del producto, para mostrarlo en el historial sin que la UI
 * tenga que cruzar listas (el cruce se hace con un JOIN en la capa de datos).
 */
data class MovimientoDetalle(
    val movimiento: Movimiento,
    val productoNombre: String
)

/** Motivo usado al registrar automaticamente el stock con el que nace un producto. */
const val MOTIVO_STOCK_INICIAL = "Stock inicial"

/** Motivos predefinidos del formulario; la lista depende del tipo de movimiento. */
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
