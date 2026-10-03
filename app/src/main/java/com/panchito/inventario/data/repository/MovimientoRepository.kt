package com.panchito.inventario.data.repository

import android.util.Log
import com.panchito.inventario.data.auth.AuthDataSource
import com.panchito.inventario.data.local.datasource.MovimientoLocalDataSource
import com.panchito.inventario.data.local.datasource.ProductoLocalDataSource
import com.panchito.inventario.data.mapper.toDomain
import com.panchito.inventario.data.mapper.toEntity
import com.panchito.inventario.data.sync.ApiSyncManager
import com.panchito.inventario.data.sync.ProgramadorDeSincronizacion
import com.panchito.inventario.data.sync.ResultadoSincronizacion
import com.panchito.inventario.domain.model.Movimiento
import com.panchito.inventario.domain.model.MovimientoDetalle
import com.panchito.inventario.domain.model.MovimientoException
import com.panchito.inventario.domain.model.TipoMovimiento
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

private const val TAG = "MovimientoRepository"

/**
 * Contrato del repositorio de Movimientos de inventario. Registrar un movimiento es la UNICA
 * forma de mover el stock de un producto.
 *
 * Fase 3: los movimientos AHORA SI se sincronizan con el servidor (movimientos.php), ademas del
 * producto cuyo stock cambio (MovimientoDao ya lo marca PENDIENTE_ACTUALIZAR al mover el stock).
 * El historial es compartido: un movimiento hecho en un dispositivo aparece en los demas despues
 * de sincronizar (ver ApiSyncManager).
 */
interface MovimientoRepository {
    fun observarMovimientos(): Flow<List<MovimientoDetalle>>

    /**
     * [fechaVencimiento] es OPCIONAL: no todos los productos vencen (ej. articulos de limpieza).
     * Cuando se indica, actualiza la fecha de vencimiento del PRODUCTO (no crea un campo nuevo en
     * el movimiento); cuando es null, se conserva la que el producto ya tenia.
     */
    suspend fun registrar(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: String,
        fechaVencimiento: Date? = null
    ): Result<Int>
}

class MovimientoRepositoryImpl(
    private val local: MovimientoLocalDataSource,
    private val productos: ProductoLocalDataSource,
    private val auth: AuthDataSource,
    private val sincronizador: ApiSyncManager,
    private val programador: ProgramadorDeSincronizacion
) : MovimientoRepository {

    override fun observarMovimientos(): Flow<List<MovimientoDetalle>> =
        local.observarConProducto().map { lista -> lista.map { it.toDomain() } }

    override suspend fun registrar(
        productoId: String,
        tipo: TipoMovimiento,
        cantidad: Int,
        motivo: String,
        fechaVencimiento: Date?
    ): Result<Int> = try {
        val uid = auth.usuarioActual()?.uid ?: throw MovimientoException.SinSesion()
        if (productoId.isBlank()) throw MovimientoException.ProductoNoSeleccionado()
        if (cantidad <= 0) throw MovimientoException.CantidadInvalida()
        if (motivo.isBlank()) throw MovimientoException.MotivoInvalido()

        val producto = productos.obtenerPorId(productoId)
            ?: throw MovimientoException.ProductoNoEncontrado()

        val nuevoStock = when (tipo) {
            TipoMovimiento.ENTRADA -> producto.stock + cantidad
            TipoMovimiento.SALIDA -> {
                if (cantidad > producto.stock) throw MovimientoException.StockInsuficiente(producto.stock)
                producto.stock - cantidad
            }
        }

        val movimiento = Movimiento(
            id = UUID.randomUUID().toString(),
            productoId = producto.id,
            ownerUid = uid,
            tipo = tipo,
            cantidad = cantidad,
            motivo = motivo.trim(),
            fechaHora = Date()
        )

        val guardado = local.registrarYActualizarStock(
            movimiento.toEntity(),
            nuevoStock,
            fechaVencimiento?.time
        )
        if (!guardado) throw MovimientoException.ProductoNoEncontrado()

        reflejarEnElServidor()

        Result.success(nuevoStock)
    } catch (e: CancellationException) {
        throw e
    } catch (e: MovimientoException) {
        Result.failure(e)
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo registrar el movimiento", e)
        Result.failure(Exception("No se pudo registrar el movimiento. Inténtalo nuevamente."))
    }

    private suspend fun reflejarEnElServidor() {
        val resultado = try {
            sincronizador.sincronizarAhora(incluirDescarga = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Fallo el intento inmediato de sincronizacion", e)
            ResultadoSincronizacion.ERROR
        }
        if (resultado != ResultadoSincronizacion.COMPLETADA) programador.solicitar()
    }
}