package com.panchito.inventario.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.panchito.inventario.data.auth.AuthDataSource
import com.panchito.inventario.data.local.datasource.MovimientoLocalDataSource
import com.panchito.inventario.data.local.datasource.ProductoLocalDataSource
import com.panchito.inventario.data.mapper.toDomain
import com.panchito.inventario.data.mapper.toEntity
import com.panchito.inventario.data.sync.ApiSyncManager
import com.panchito.inventario.data.sync.ProgramadorDeSincronizacion
import com.panchito.inventario.data.sync.ResultadoSincronizacion
import com.panchito.inventario.domain.model.EstadoSincronizacion
import com.panchito.inventario.domain.model.Producto
import com.panchito.inventario.domain.model.ProductoException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Implementacion del repositorio de productos.
 * - [local] (Room): base local del INVENTARIO COMPARTIDO real del minimarket, con el CRUD completo.
 *   Fase 2: el inventario ya NO se separa por usuario (ownerUid solo queda como dato de
 *   trazabilidad de quien creo/edito cada producto; ver [ProductoLocalDataSource]).
 * - [sincronizador] (ApiSyncManager): refleja las escrituras en tu API REST PHP propia. Room
 *   siempre se escribe primero; si el servidor falla, el cambio ya quedo en Room y se informa
 *   con [EstadoNube], sin revertir ni bloquear la operacion.
 */
class ProductoRepositoryImpl(
    private val local: ProductoLocalDataSource,
    private val auth: AuthDataSource,
    private val movimientos: MovimientoLocalDataSource,
    private val sincronizador: ApiSyncManager,
    private val programador: ProgramadorDeSincronizacion
) : ProductoRepository {

    override fun observarCatalogo(): Flow<List<Producto>> =
        local.observarActivos().map { lista -> lista.map { it.toDomain() } }

    override fun observarProducto(id: String): Flow<Producto?> =
        local.observarPorId(id).map { it?.toDomain() }

    override suspend fun obtenerProducto(id: String): Producto? =
        local.obtenerPorId(id)?.toDomain()

    override suspend fun registrar(producto: Producto): Result<ResultadoConNube<String>> = ejecutar {
        val uid = uidActual()
        // Fase 5: el codigo YA NO lo escribe el usuario, se genera automaticamente (PRO0001,
        // PRO0002, ...); y todo producto nuevo nace con stock = 0 (el stock se carga despues con
        // un movimiento de ENTRADA, no al crear el producto).
        val codigoGenerado = generarSiguienteCodigo()
        val candidato = producto.copy(codigo = codigoGenerado, stock = 0)
        validar(candidato)
        // Id ESTABLE generado aqui una sola vez: el mismo se usa en Room y en MySQL. Se guarda
        // PRIMERO en Room y como PENDIENTE_CREAR: la operacion es exitosa aunque no haya
        // Internet, y la subida queda a cargo del sincronizador.
        val nuevo = candidato.copy(
            id = UUID.randomUUID().toString(),
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE_CREAR,
            updatedAt = System.currentTimeMillis(),
            deletedAt = null
        )
        local.insertar(nuevo.toEntity(uid))
        // Fase 5: ya no se crea un movimiento de "Stock inicial" (el stock siempre nace en 0).
        ResultadoConNube(nuevo.id, reflejarEnElServidor())
    }

    override suspend fun actualizar(producto: Producto): Result<ResultadoConNube<Unit>> = ejecutar {
        val uid = uidActual()
        validar(producto)
        val existente = local.obtenerPorId(producto.id) ?: throw ProductoException.NoEncontrado()
        val conMismoCodigo = local.buscarPorCodigo(producto.codigo.trim())
        if (conMismoCodigo != null && conMismoCodigo.id != producto.id) {
            throw ProductoException.CodigoDuplicado(producto.codigo.trim())
        }
        local.actualizar(
            producto.copy(
                estadoSincronizacion = estadoTrasEditar(existente.estadoSincronizacion),
                updatedAt = System.currentTimeMillis(),
                deletedAt = null
            ).toEntity(uid)
        )
        ResultadoConNube(Unit, reflejarEnElServidor())
    }

    override suspend fun eliminar(id: String): Result<ResultadoConNube<ResultadoEliminacion>> = ejecutar {
        val uid = uidActual()
        val existente = local.obtenerPorId(id) ?: throw ProductoException.NoEncontrado()
        val ahora = System.currentTimeMillis()
        if (local.contarMovimientos(id) > 0) {
            // Regla de negocio: un producto con movimientos asociados se desactiva, no se borra.
            local.actualizar(
                existente.copy(
                    activo = false,
                    updatedAt = ahora,
                    estadoSincronizacion = estadoTrasEditar(existente.estadoSincronizacion).name
                )
            )
            ResultadoConNube(ResultadoEliminacion.DESACTIVADO, reflejarEnElServidor())
        } else {
            // Borrado LOGICO: la fila se conserva como lapida (deletedAt) para sincronizar la
            // eliminacion aunque ahora no haya Internet y para no romper la relacion de los
            // movimientos historicos con su productoId.
            local.actualizar(
                existente.copy(
                    deletedAt = ahora,
                    updatedAt = ahora,
                    estadoSincronizacion = EstadoSincronizacion.PENDIENTE_ELIMINAR.name
                )
            )
            ResultadoConNube(ResultadoEliminacion.ELIMINADO, reflejarEnElServidor())
        }
    }

    /**
     * Fase 5: genera el siguiente codigo secuencial con formato "PRO0001", "PRO0002", etc.,
     * a partir del ultimo codigo con ese formato que exista en el inventario COMPARTIDO
     * (ver ProductoDao.obtenerUltimoCodigoGenerado, que incluye lapidas eliminadas para no
     * reciclar un numero ya usado).
     *
     * LIMITE CONOCIDO (documentado tambien en el informe final): esta generacion es LOCAL a cada
     * dispositivo. Si dos dispositivos crean un producto NUEVO al mismo tiempo mientras ambos
     * estan sin Internet, podrian generar el mismo codigo; el backend (movimientos.php /
     * productos.php) detecta la colision al sincronizar y devuelve 409, pero esta version no
     * reintenta automaticamente con el siguiente numero. Para el uso normal (un dispositivo a la
     * vez, con sincronizacion frecuente) no deberia ocurrir.
     */
    private suspend fun generarSiguienteCodigo(): String {
        val ultimo = local.obtenerUltimoCodigoGenerado()
        val siguienteNumero = ultimo
            ?.removePrefix("PRO")
            ?.toIntOrNull()
            ?.plus(1)
            ?: 1
        return "PRO" + siguienteNumero.toString().padStart(4, '0')
    }

    private fun uidActual(): String =
        auth.usuarioActual()?.uid ?: throw ProductoException.SinSesion()

    private fun estadoTrasEditar(estadoActual: String): EstadoSincronizacion =
        if (estadoActual == EstadoSincronizacion.PENDIENTE_CREAR.name) EstadoSincronizacion.PENDIENTE_CREAR
        else EstadoSincronizacion.PENDIENTE_ACTUALIZAR

    /**
     * El cambio YA esta guardado en Room. Esto solo intenta reflejarlo de inmediato en el
     * servidor; si no se puede, se encola el Worker y el registro queda pendiente. Nunca falla
     * ni revierte nada: lo peor que pasa es que el usuario vea "pendiente de sincronizacion".
     */
    private suspend fun reflejarEnElServidor(): EstadoNube {
        val resultado = try {
            sincronizador.sincronizarAhora(incluirDescarga = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ProductoRepository", "Fallo el intento inmediato de sincronizacion", e)
            ResultadoSincronizacion.ERROR
        }
        if (resultado != ResultadoSincronizacion.COMPLETADA) programador.solicitar()
        return when (resultado) {
            ResultadoSincronizacion.COMPLETADA -> EstadoNube.GUARDADO
            ResultadoSincronizacion.SIN_CONEXION -> EstadoNube.SIN_CONEXION
            ResultadoSincronizacion.SIN_SESION -> EstadoNube.SIN_SESION
            ResultadoSincronizacion.ERROR -> EstadoNube.ERROR
        }
    }

    private fun validar(producto: Producto) {
        if (producto.codigo.isBlank()) throw ProductoException.DatosInvalidos("El codigo es obligatorio")
        if (producto.nombre.isBlank()) throw ProductoException.DatosInvalidos("El nombre es obligatorio")
        if (producto.precio < 0) throw ProductoException.DatosInvalidos("El precio no puede ser negativo")
        if (producto.stock < 0) throw ProductoException.DatosInvalidos("El stock no puede ser negativo")
        if (producto.stockMinimo < 0) throw ProductoException.DatosInvalidos("El stock minimo no puede ser negativo")
    }

    private inline fun <T> ejecutar(accion: () -> T): Result<T> = try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: SQLiteConstraintException) {
        Result.failure(ProductoException.Restriccion())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
