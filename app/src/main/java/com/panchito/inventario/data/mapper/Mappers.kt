package com.panchito.inventario.data.mapper

import com.panchito.inventario.data.local.entity.CategoriaEntity
import com.panchito.inventario.data.local.entity.MovimientoConProducto
import com.panchito.inventario.data.local.entity.MovimientoEntity
import com.panchito.inventario.data.local.entity.ProductoEntity
import com.panchito.inventario.data.remote.dto.CategoriaDto
import com.panchito.inventario.data.remote.dto.MovimientoDto
import com.panchito.inventario.data.remote.dto.ProductoDto
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.domain.model.EstadoSincronizacion
import com.panchito.inventario.domain.model.Movimiento
import com.panchito.inventario.domain.model.MovimientoDetalle
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.domain.model.TipoMovimiento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Conversiones Entity (capa de datos) <-> Modelo de dominio.
 * La UI y los ViewModel solo conocen los modelos de dominio; las entidades de Room (o los DTO de
 * Retrofit) quedan dentro de la capa de datos.
 */
fun ProductoEntity.toDomain(): Producto = Producto(
    id = id,
    codigo = codigo,
    nombre = nombre,
    categoriaId = categoriaId,
    precio = precio,
    stock = stock,
    stockMinimo = stockMinimo,
    fechaVencimiento = fechaVencimientoMillis?.let { Date(it) },
    activo = activo,
    estadoSincronizacion = estadoSincronizacion.comoEstadoSincronizacion(),
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

/**
 * La entidad de Room necesita el [ownerUid] del usuario autenticado (el modelo de dominio no lo
 * lleva, porque la UI nunca trabaja con productos de otro usuario).
 */
fun Producto.toEntity(ownerUid: String): ProductoEntity = ProductoEntity(
    id = id,
    ownerUid = ownerUid,
    codigo = codigo.trim(),
    nombre = nombre.trim(),
    categoriaId = categoriaId,
    precio = precio,
    stock = stock,
    stockMinimo = stockMinimo,
    fechaVencimientoMillis = fechaVencimiento?.time,
    activo = activo,
    estadoSincronizacion = estadoSincronizacion.name,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun CategoriaEntity.toDomain(): Categoria = Categoria(
    id = id,
    nombre = nombre,
    activa = activa,
    estadoSincronizacion = estadoSincronizacion.comoEstadoSincronizacion()
)

/** JSON de categorias.php <-> Room. El id es el mismo en el dispositivo y en MySQL. */
fun CategoriaEntity.toApiDto(): CategoriaDto = CategoriaDto(
    id = id,
    nombre = nombre.trim(),
    activa = activa,
    updatedAt = updatedAt
)

/** Lo que viene del servidor esta, por definicion, sincronizado. */
fun CategoriaDto.toEntity(): CategoriaEntity = CategoriaEntity(
    id = id,
    nombre = nombre.trim(),
    activa = activa,
    estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO.name,
    updatedAt = updatedAt
)

/**
 * Conversiones ProductoDto (JSON de tu API PHP, ver htdocs/inventario_api/productos.php) <->
 * modelo de dominio. El backend guarda EXACTAMENTE los mismos campos que Room, asi que el mapeo
 * es directo; la unica conversion real es la fecha de vencimiento, que MySQL guarda como DATE
 * ("YYYY-MM-DD") y Room como millis UTC del dia (ver Formatos.kt: aMillisUtcDelDia).
 */
fun ProductoDto.toDomain(): Producto = Producto(
    id = id,
    codigo = codigo,
    nombre = nombre,
    categoriaId = categoriaId,
    precio = precio,
    stock = stock,
    stockMinimo = stockMinimo,
    fechaVencimiento = fechaVencimiento?.let { parsearFechaApi(it) },
    activo = activo,
    // Lo que viene del servidor esta, por definicion, sincronizado.
    estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

/** [ownerUid] no vive en el modelo de dominio; lo aporta quien llama (ApiSyncManager). */
fun Producto.toApiDto(ownerUid: String): ProductoDto = ProductoDto(
    id = id,
    ownerUid = ownerUid,
    codigo = codigo.trim(),
    nombre = nombre.trim(),
    categoriaId = categoriaId,
    precio = precio,
    stock = stock,
    stockMinimo = stockMinimo,
    fechaVencimiento = fechaVencimiento?.let { formatearFechaApi(it) },
    activo = activo,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

/** yyyy-MM-dd en UTC: coincide con el criterio que ya usa el DatePicker (ver Formatos.kt). */
private fun formatearFechaApi(fecha: Date): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(fecha)

private fun parsearFechaApi(texto: String): Date? =
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(texto)
    }.getOrNull()

/**
 * Conversiones Movimiento. El tipo y el estado de sincronizacion se guardan como texto en Room y
 * se convierten aqui a enums del dominio; si el texto no fuera valido se usa un valor por defecto
 * en lugar de lanzar una excepcion, para que un dato viejo no rompa la pantalla.
 */
fun MovimientoEntity.toDomain(): Movimiento = Movimiento(
    id = id,
    productoId = productoId,
    ownerUid = ownerUid,
    tipo = runCatching { TipoMovimiento.valueOf(tipo) }.getOrDefault(TipoMovimiento.ENTRADA),
    cantidad = cantidad,
    motivo = motivo,
    fechaHora = Date(fechaHoraMillis),
    estadoSincronizacion = estadoSincronizacion.comoEstadoSincronizacion(),
    updatedAt = updatedAt
)

fun Movimiento.toEntity(): MovimientoEntity = MovimientoEntity(
    id = id,
    productoId = productoId,
    ownerUid = ownerUid,
    tipo = tipo.name,
    cantidad = cantidad,
    motivo = motivo.trim(),
    fechaHoraMillis = fechaHora.time,
    estadoSincronizacion = estadoSincronizacion.name,
    updatedAt = updatedAt
)

fun MovimientoConProducto.toDomain(): MovimientoDetalle = MovimientoDetalle(
    movimiento = movimiento.toDomain(),
    productoNombre = productoNombre
)

/** Si el valor no fuera reconocible (dato antiguo), se asume SINCRONIZADO en vez de lanzar. */
private fun String.comoEstadoSincronizacion(): EstadoSincronizacion =
    runCatching { EstadoSincronizacion.valueOf(this) }.getOrDefault(EstadoSincronizacion.SINCRONIZADO)

/**
 * Conversiones MovimientoDto (JSON de movimientos.php) <-> modelo de dominio (Fase 3: sincronizacion
 * de movimientos). [ownerUid] no viaja en el modelo de dominio del movimiento remoto descargado: se
 * usa el creadoPorUid que trae el propio DTO.
 */
fun MovimientoDto.toDomain(): Movimiento = Movimiento(
    id = id,
    productoId = productoId,
    ownerUid = creadoPorUid,
    tipo = runCatching { TipoMovimiento.valueOf(tipo) }.getOrDefault(TipoMovimiento.ENTRADA),
    cantidad = cantidad,
    motivo = motivo,
    fechaHora = Date(fechaHora),
    estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
    updatedAt = updatedAt
)

/** [ownerUid] lo aporta quien llama (ApiSyncManager): es el usuario que registro el movimiento. */
fun Movimiento.toApiDto(ownerUid: String): MovimientoDto = MovimientoDto(
    id = id,
    productoId = productoId,
    creadoPorUid = ownerUid,
    tipo = tipo.name,
    cantidad = cantidad,
    motivo = motivo.trim(),
    fechaHora = fechaHora.time,
    updatedAt = updatedAt
)