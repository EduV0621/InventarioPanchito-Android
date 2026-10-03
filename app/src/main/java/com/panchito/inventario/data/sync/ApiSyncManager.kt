package com.panchito.inventario.data.sync

import android.util.Log
import com.panchito.inventario.data.auth.AuthDataSource
import com.panchito.inventario.data.local.datasource.CategoriaLocalDataSource
import com.panchito.inventario.data.local.datasource.MovimientoLocalDataSource
import com.panchito.inventario.data.local.datasource.ProductoLocalDataSource
import com.panchito.inventario.data.mapper.toApiDto
import com.panchito.inventario.data.mapper.toDomain
import com.panchito.inventario.data.mapper.toEntity
import com.panchito.inventario.data.remote.ConectividadProvider
import com.panchito.inventario.data.remote.RemotoException
import com.panchito.inventario.data.remote.datasource.CategoriaRemoteDataSource
import com.panchito.inventario.data.remote.datasource.MovimientoRemoteDataSource
import com.panchito.inventario.data.remote.datasource.ProductoRemoteDataSource
import com.panchito.inventario.domain.model.EstadoSincronizacion
import kotlinx.coroutines.CancellationException

private const val TAG = "ApiSync"

enum class ResultadoSincronizacion {
    SIN_SESION,

    SIN_CONEXION,

    COMPLETADA,

    ERROR
}

class ApiSyncManager(
    private val categoriasLocal: CategoriaLocalDataSource,
    private val categoriasRemoto: CategoriaRemoteDataSource,
    private val productosLocal: ProductoLocalDataSource,
    private val productosRemoto: ProductoRemoteDataSource,
    private val movimientosLocal: MovimientoLocalDataSource,
    private val movimientosRemoto: MovimientoRemoteDataSource,
    private val auth: AuthDataSource,
    private val conectividad: ConectividadProvider
) {
    suspend fun sincronizarAhora(incluirDescarga: Boolean = true): ResultadoSincronizacion {
        val uid = auth.usuarioActual()?.uid ?: return ResultadoSincronizacion.SIN_SESION
        if (!conectividad.hayConexion()) return ResultadoSincronizacion.SIN_CONEXION

        var huboError = false
        var sinConexion = false
        fun anotar(fallo: Fallo) {
            if (fallo == Fallo.SIN_CONEXION) sinConexion = true else if (fallo == Fallo.ERROR) huboError = true
        }

        anotar(subirCategorias())
        if (!sinConexion) anotar(subirProductos(uid))
        if (!sinConexion) anotar(subirMovimientos(uid))
        if (!sinConexion && incluirDescarga) {
            anotar(descargarCategorias())
            if (!sinConexion) anotar(descargarProductos())
            if (!sinConexion) anotar(descargarMovimientos())
        }

        return when {
            sinConexion -> ResultadoSincronizacion.SIN_CONEXION
            huboError -> ResultadoSincronizacion.ERROR
            else -> ResultadoSincronizacion.COMPLETADA
        }
    }

    private enum class Fallo { NINGUNO, SIN_CONEXION, ERROR }

    private suspend fun subirCategorias(): Fallo {
        var fallo = Fallo.NINGUNO
        val pendientes = try {
            categoriasLocal.obtenerPendientes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron leer las categorias pendientes", e)
            return Fallo.ERROR
        }

        for (entidad in pendientes) {
            try {
                categoriasRemoto.guardarCategoria(entidad.toApiDto())
                categoriasLocal.marcarSincronizado(entidad.id, entidad.updatedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RemotoException.SinConexion) {
                Log.w(TAG, "Sin conexion subiendo la categoria ${entidad.id}; queda pendiente")
                return Fallo.SIN_CONEXION
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo subir la categoria ${entidad.id}", e)
                fallo = Fallo.ERROR
            }
        }
        return fallo
    }

    private suspend fun subirProductos(uid: String): Fallo {
        var fallo = Fallo.NINGUNO
        val pendientes = try {
            productosLocal.obtenerPendientes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron leer los productos pendientes", e)
            return Fallo.ERROR
        }

        for (entidad in pendientes) {
            val producto = entidad.toDomain()
            try {
                when (entidad.estadoSincronizacion) {
                    EstadoSincronizacion.PENDIENTE_CREAR.name -> productosRemoto.crearProducto(producto.toApiDto(uid))
                    EstadoSincronizacion.PENDIENTE_ACTUALIZAR.name -> productosRemoto.actualizarProducto(entidad.id, producto.toApiDto(uid))
                    EstadoSincronizacion.PENDIENTE_ELIMINAR.name -> productosRemoto.eliminarProducto(entidad.id)
                    else -> continue
                }

                productosLocal.marcarSincronizado(entidad.id, entidad.updatedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RemotoException.SinConexion) {
                Log.w(TAG, "Sin conexion subiendo el producto ${entidad.id}; queda pendiente")
                return Fallo.SIN_CONEXION
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo subir el producto ${entidad.id}", e)
                fallo = Fallo.ERROR
            }
        }
        return fallo
    }

    private suspend fun subirMovimientos(uid: String): Fallo {
        var fallo = Fallo.NINGUNO
        val pendientes = try {
            movimientosLocal.obtenerPendientes()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron leer los movimientos pendientes", e)
            return Fallo.ERROR
        }

        for (entidad in pendientes) {
            try {
                movimientosRemoto.crearMovimiento(entidad.toDomain().toApiDto(uid))
                movimientosLocal.marcarSincronizado(entidad.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RemotoException.SinConexion) {
                Log.w(TAG, "Sin conexion subiendo el movimiento ${entidad.id}; queda pendiente")
                return Fallo.SIN_CONEXION
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo subir el movimiento ${entidad.id}", e)
                fallo = Fallo.ERROR
            }
        }
        return fallo
    }

    private suspend fun descargarCategorias(): Fallo {
        var fallo = Fallo.NINGUNO
        try {
            for (remoto in categoriasRemoto.obtenerCategorias()) {
                if (remoto.id <= 0L) continue
                try {
                    val local = categoriasLocal.buscarPorId(remoto.id)
                    when {
                        local == null -> categoriasLocal.insertarIgnorando(remoto.toEntity())

                        local.estadoSincronizacion != EstadoSincronizacion.SINCRONIZADO.name -> Unit

                        remoto.updatedAt > local.updatedAt -> categoriasLocal.actualizar(remoto.toEntity())
                        else -> Unit
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo guardar la categoria remota ${remoto.id}", e)
                    fallo = Fallo.ERROR
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RemotoException.SinConexion) {
            return Fallo.SIN_CONEXION
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron descargar las categorias", e)
            fallo = Fallo.ERROR
        }
        return fallo
    }

    private suspend fun descargarProductos(): Fallo {
        var fallo = Fallo.NINGUNO
        try {
            for (remoto in productosRemoto.obtenerProductos()) {
                if (remoto.id.isBlank()) continue
                try {
                    val local = productosLocal.obtenerPorIdIncluyendoEliminados(remoto.id)
                    val remotoDomain = remoto.toDomain()
                    when {
                        local == null -> productosLocal.insertarIgnorando(remotoDomain.toEntity(remoto.ownerUid))

                        local.estadoSincronizacion != EstadoSincronizacion.SINCRONIZADO.name -> Unit

                        remotoDomain.updatedAt > local.updatedAt -> productosLocal.actualizar(remotoDomain.toEntity(remoto.ownerUid))
                        else -> Unit
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo guardar el producto remoto ${remoto.id}", e)
                    fallo = Fallo.ERROR
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RemotoException.SinConexion) {
            return Fallo.SIN_CONEXION
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron descargar los productos", e)
            fallo = Fallo.ERROR
        }
        return fallo
    }

    private suspend fun descargarMovimientos(): Fallo {
        var fallo = Fallo.NINGUNO
        try {
            for (remoto in movimientosRemoto.obtenerMovimientos()) {
                if (remoto.id.isBlank()) continue
                try {
                    val local = movimientosLocal.obtenerPorId(remoto.id)
                    if (local == null) {
                        movimientosLocal.insertarIgnorando(remoto.toDomain().toEntity())
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo guardar el movimiento remoto ${remoto.id}", e)
                    fallo = Fallo.ERROR
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: RemotoException.SinConexion) {
            return Fallo.SIN_CONEXION
        } catch (e: Exception) {
            Log.e(TAG, "No se pudieron descargar los movimientos", e)
            fallo = Fallo.ERROR
        }
        return fallo
    }
}
