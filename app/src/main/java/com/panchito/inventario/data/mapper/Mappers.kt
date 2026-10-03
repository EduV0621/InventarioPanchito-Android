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

fun CategoriaEntity.toApiDto(): CategoriaDto = CategoriaDto(
    id = id,
    nombre = nombre.trim(),
    activa = activa,
    updatedAt = updatedAt
)

fun CategoriaDto.toEntity(): CategoriaEntity = CategoriaEntity(
    id = id,
    nombre = nombre.trim(),
    activa = activa,
    estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO.name,
    updatedAt = updatedAt
)

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

    estadoSincronizacion = EstadoSincronizacion.SINCRONIZADO,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

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

private fun formatearFechaApi(fecha: Date): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(fecha)

private fun parsearFechaApi(texto: String): Date? =
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(texto)
    }.getOrNull()

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

private fun String.comoEstadoSincronizacion(): EstadoSincronizacion =
    runCatching { EstadoSincronizacion.valueOf(this) }.getOrDefault(EstadoSincronizacion.SINCRONIZADO)

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
