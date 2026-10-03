package com.panchito.inventario.data.repository

import com.panchito.inventario.domain.model.Producto
import kotlinx.coroutines.flow.Flow

/** Resultado de eliminar un producto: se borra fisicamente o, si tiene movimientos, se desactiva. */
enum class ResultadoEliminacion { ELIMINADO, DESACTIVADO }

/**
 * Que paso con la copia en el SERVIDOR (API REST PHP + MySQL) de una escritura del inventario
 * real. Room siempre se escribe primero; si el servidor no se pudo actualizar, el cambio queda
 * solo en el dispositivo y la UI se lo indica al usuario. La subida pendiente la reintenta
 * ApiSyncManager (al toque despues de la escritura y, si eso falla, en segundo plano con
 * WorkManager).
 */
enum class EstadoNube { GUARDADO, SIN_CONEXION, SIN_SESION, ERROR }

/** Resultado de una escritura del inventario real: el [valor] local mas el [nube] del servidor. */
data class ResultadoConNube<out T>(val valor: T, val nube: EstadoNube)

/**
 * Contrato del repositorio de Productos. Es la unica fuente de verdad para este dominio: los
 * ViewModel solo dependen de esta interfaz y nunca conocen Room ni Retrofit directamente.
 *
 * - LOCAL (Room): base local del inventario del minimarket. Es la que lee la UI (Catalogo,
 *   Detalle, Dashboard) y la primera en escribirse; funciona siempre, con o sin Internet.
 * - SERVIDOR (API REST propia, PHP + MySQL): cada escritura (registrar / actualizar / eliminar)
 *   se intenta reflejar tambien alli; el resultado lo informa [EstadoNube]. Si falla o no hay
 *   Internet, el cambio queda pendiente y ApiSyncManager lo reintenta mas tarde.
 */
interface ProductoRepository {
    fun observarCatalogo(): Flow<List<Producto>>

    fun observarProducto(id: String): Flow<Producto?>

    suspend fun obtenerProducto(id: String): Producto?

    // Requieren un usuario autenticado (ProductoException.SinSesion si no lo hay).

    suspend fun registrar(producto: Producto): Result<ResultadoConNube<String>>

    suspend fun actualizar(producto: Producto): Result<ResultadoConNube<Unit>>

    suspend fun eliminar(id: String): Result<ResultadoConNube<ResultadoEliminacion>>
}