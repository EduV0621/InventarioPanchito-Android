package com.panchito.inventario.data.local.dao

import androidx.room.*
import com.panchito.inventario.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Fase 2 (INVENTARIO COMPARTIDO): las consultas de productos YA NO filtran por ownerUid. El
 * Minimarket Panchito tiene un unico catalogo, visible por igual para el Administrador y para
 * todos los Empleados. La columna ownerUid se conserva en la tabla (ver ProductoEntity) solo
 * como dato de trazabilidad de quien creo/edito cada producto, nunca como filtro de acceso.
 */
@Dao
interface ProductoDao {
    /** Todos los productos del minimarket, incluidos los desactivados (no las lapidas eliminadas). */
    @Query("SELECT * FROM productos WHERE deletedAt IS NULL ORDER BY nombre ASC")
    fun observarTodos(): Flow<List<ProductoEntity>>

    /** Catalogo visible: solo productos activos. */
    @Query("SELECT * FROM productos WHERE deletedAt IS NULL AND activo = 1 ORDER BY nombre ASC")
    fun observarActivos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE deletedAt IS NULL AND stock <= stockMinimo AND activo = 1")
    fun observarStockBajo(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun observarPorId(id: String): Flow<ProductoEntity?>

    @Query("SELECT * FROM productos WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun obtenerPorId(id: String): ProductoEntity?

    /** Incluye las lapidas (deletedAt != null); la usa la sincronizacion, no el catalogo. */
    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorIdIncluyendoEliminados(id: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE codigo = :codigo AND deletedAt IS NULL LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): ProductoEntity?

    /**
     * Fase 5 (codigo automatico): ultimo codigo con el formato "PRO####" que exista en el
     * inventario COMPLETO (incluidas lapidas eliminadas, para no reciclar un numero ya usado).
     * Se ordena por longitud y luego alfabeticamente para que "PRO10" quede despues de "PRO9".
     */
    @Query("SELECT codigo FROM productos WHERE codigo LIKE 'PRO%' ORDER BY LENGTH(codigo) DESC, codigo DESC LIMIT 1")
    suspend fun obtenerUltimoCodigoGenerado(): String?

    /** Cantidad de movimientos que referencian al producto (regla: con movimientos no se elimina, se desactiva). */
    @Query("SELECT COUNT(*) FROM movimientos WHERE productoId = :productoId")
    suspend fun contarMovimientos(productoId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(producto: ProductoEntity)

    /** Usada al descargar del servidor: si la fila ya existe no se toca (se decide antes que hacer). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarIgnorando(producto: ProductoEntity): Long

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    @Delete
    suspend fun eliminar(producto: ProductoEntity)

    // ---- Sincronizacion con la API PHP/MySQL ----

    /** Registros de TODO el inventario compartido que todavia no estan reflejados en el servidor. */
    @Query("SELECT * FROM productos WHERE estadoSincronizacion != 'SINCRONIZADO'")
    suspend fun obtenerPendientes(): List<ProductoEntity>

    /**
     * Marca el registro como ya enviado, PERO solo si no volvio a cambiar mientras se subia
     * (updatedAt debe seguir siendo el mismo que se envio). Asi una edicion hecha durante la
     * sincronizacion no se pierde marcandola como sincronizada por error.
     */
    @Query(
        """
        UPDATE productos SET estadoSincronizacion = 'SINCRONIZADO'
        WHERE id = :id AND updatedAt = :updatedAt
        """
    )
    suspend fun marcarSincronizado(id: String, updatedAt: Long): Int
}
