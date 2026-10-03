package com.panchito.inventario.data.repository

import android.util.Log
import com.panchito.inventario.data.local.datasource.CategoriaLocalDataSource
import com.panchito.inventario.data.local.entity.CategoriaEntity
import com.panchito.inventario.data.mapper.toDomain
import com.panchito.inventario.data.sync.ApiSyncManager
import com.panchito.inventario.data.sync.ProgramadorDeSincronizacion
import com.panchito.inventario.data.sync.ResultadoSincronizacion
import com.panchito.inventario.domain.model.Categoria
import com.panchito.inventario.domain.model.CategoriaException
import com.panchito.inventario.domain.model.EstadoSincronizacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "CategoriaRepository"

/**
 * Contrato del repositorio de categorias. Igual que productos y movimientos: Room se escribe
 * SIEMPRE primero (por eso se puede registrar, editar o desactivar sin Internet) y despues se
 * intenta reflejar en el servidor; el [EstadoNube] del resultado dice que paso con esa copia.
 */
interface CategoriaRepository {
    /** Todas las categorias ordenadas por nombre (incluye inactivas; filtrar por Categoria.activa si hace falta). */
    fun observarCategorias(): Flow<List<Categoria>>

    /** Registra una categoria nueva. Falla con [CategoriaException.NombreDuplicado] si el nombre ya existe. */
    suspend fun registrar(nombre: String): Result<ResultadoConNube<Categoria>>

    /** Edita el nombre de una categoria existente. */
    suspend fun actualizar(id: Long, nombre: String): Result<ResultadoConNube<Categoria>>

    /**
     * Activa o desactiva una categoria (borrado logico: nunca se elimina de la base de datos,
     * solo se marca como inactiva para que deje de aparecer como opcion al registrar productos).
     */
    suspend fun cambiarEstado(id: Long, activa: Boolean): Result<ResultadoConNube<Categoria>>
}

class CategoriaRepositoryImpl(
    private val local: CategoriaLocalDataSource,
    private val sincronizador: ApiSyncManager,
    private val programador: ProgramadorDeSincronizacion
) : CategoriaRepository {

    override fun observarCategorias(): Flow<List<Categoria>> =
        local.observarTodas().map { lista -> lista.map { it.toDomain() } }

    override suspend fun registrar(nombre: String): Result<ResultadoConNube<Categoria>> = ejecutar {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) throw CategoriaException.NombreVacio()
        if (local.buscarPorNombre(nombreLimpio) != null) throw CategoriaException.NombreDuplicado(nombreLimpio)

        val nueva = CategoriaEntity(
            nombre = nombreLimpio,
            activa = true,
            estadoSincronizacion = EstadoSincronizacion.PENDIENTE_CREAR.name,
            updatedAt = System.currentTimeMillis()
        )
        val id = local.insertar(nueva)
        ResultadoConNube(nueva.copy(id = id).toDomain(), reflejarEnElServidor())
    }

    override suspend fun actualizar(id: Long, nombre: String): Result<ResultadoConNube<Categoria>> = ejecutar {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) throw CategoriaException.NombreVacio()
        val existente = local.buscarPorId(id) ?: throw CategoriaException.NoEncontrada()
        val duplicada = local.buscarPorNombre(nombreLimpio)
        if (duplicada != null && duplicada.id != id) throw CategoriaException.NombreDuplicado(nombreLimpio)

        val actualizada = existente.copy(
            nombre = nombreLimpio,
            estadoSincronizacion = estadoTrasEditar(existente.estadoSincronizacion),
            updatedAt = System.currentTimeMillis()
        )
        local.actualizar(actualizada)
        ResultadoConNube(actualizada.toDomain(), reflejarEnElServidor())
    }

    override suspend fun cambiarEstado(id: Long, activa: Boolean): Result<ResultadoConNube<Categoria>> = ejecutar {
        val existente = local.buscarPorId(id) ?: throw CategoriaException.NoEncontrada()
        val actualizada = existente.copy(
            activa = activa,
            estadoSincronizacion = estadoTrasEditar(existente.estadoSincronizacion),
            updatedAt = System.currentTimeMillis()
        )
        local.actualizar(actualizada)
        ResultadoConNube(actualizada.toDomain(), reflejarEnElServidor())
    }

    /** Una categoria que aun no llego al servidor sigue siendo "por crear" aunque se edite. */
    private fun estadoTrasEditar(estadoActual: String): String =
        if (estadoActual == EstadoSincronizacion.PENDIENTE_CREAR.name) EstadoSincronizacion.PENDIENTE_CREAR.name
        else EstadoSincronizacion.PENDIENTE_ACTUALIZAR.name

    /**
     * El cambio YA esta guardado en Room. Esto solo intenta reflejarlo de inmediato en el servidor;
     * si no se puede (sin Internet, servidor apagado...), se encola el Worker y queda pendiente
     * hasta que haya conexion. Nunca falla ni revierte nada.
     */
    private suspend fun reflejarEnElServidor(): EstadoNube {
        val resultado = try {
            sincronizador.sincronizarAhora(incluirDescarga = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Fallo el intento inmediato de sincronizacion", e)
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

    private inline fun <T> ejecutar(accion: () -> T): Result<T> = try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
