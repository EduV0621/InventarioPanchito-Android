package com.panchito.inventario.data.repository

import com.panchito.inventario.domain.model.Producto
import kotlinx.coroutines.flow.Flow

enum class ResultadoEliminacion { ELIMINADO, DESACTIVADO }

enum class EstadoNube { GUARDADO, SIN_CONEXION, SIN_SESION, ERROR }

data class ResultadoConNube<out T>(val valor: T, val nube: EstadoNube)

interface ProductoRepository {
    fun observarCatalogo(): Flow<List<Producto>>

    fun observarProducto(id: String): Flow<Producto?>

    suspend fun obtenerProducto(id: String): Producto?

    suspend fun registrar(producto: Producto): Result<ResultadoConNube<String>>

    suspend fun actualizar(producto: Producto): Result<ResultadoConNube<Unit>>

    suspend fun eliminar(id: String): Result<ResultadoConNube<ResultadoEliminacion>>
}
