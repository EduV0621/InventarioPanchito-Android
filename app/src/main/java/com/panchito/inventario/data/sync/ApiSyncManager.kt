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

/** Como termino un intento de sincronizacion. */
enum class ResultadoSincronizacion {
    /** No hay usuario autenticado: no hay nada que sincronizar ni a donde. */
    SIN_SESION,
    /** No hay Internet (o no se alcanza el servidor): ni se intento. */
    SIN_CONEXION,
    /** Todo lo pendiente se envio (y, si se pidio, se descargaron los cambios del servidor). */
    COMPLETADA,
    /** Algo fallo; lo pendiente sigue pendiente y conviene reintentar mas tarde. */
    ERROR
}

/**
 * UNICO componente responsable de sincronizar Room <-> tu API REST PHP/MySQL propia.
 *
 * - Room es la fuente de verdad de la app. La sincronizacion solo REFLEJA en el servidor lo que
 *   ya esta guardado localmente; nunca recalcula stock ni vuelve a aplicar movimientos.
 * - Los identificadores son los UUID que ya existen en Room: la fila remota se identifica con el
 *   mismo id (ON DUPLICATE KEY UPDATE en productos.php / INSERT unico en movimientos.php), asi
 *   que reintentar una sincronizacion actualiza (o ignora si ya existe), nunca duplica.
 * - Politica de conflictos de PRODUCTOS: "gana el mas reciente" (last write wins por updatedAt).
 *   Un cambio local todavia pendiente NUNCA se sobrescribe con la version del servidor, aunque
 *   esta sea mas reciente: ese cambio local aun no tuvo oportunidad de subir.
 * - Fase 2 (INVENTARIO COMPARTIDO): ni la subida ni la descarga de productos se filtran por
 *   owner_uid; se sube/baja el catalogo COMPLETO, compartido por Administrador y Empleados.
 * - CATEGORIAS: se suben (alta, edicion y cambio activa/inactiva, todo con un unico POST idempotente)
 *   y se descargan igual que los productos (gana el mas reciente; un cambio local pendiente no se pisa).
 *   Van SIEMPRE antes que los productos: un producto referencia a su categoria, tanto en el servidor
 *   (subida) como en Room (descarga, clave foranea).
 * - Fase 3 (MOVIMIENTOS): igual que productos, se suben/descargan sin filtrar por owner_uid, pero
 *   son INMUTABLES en el servidor: solo existe "crear" (POST). Al descargar, un movimiento que ya
 *   existe localmente (por id) se ignora; nunca se actualiza ni se borra.
 */
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

    /**
     * @param incluirDescarga false cuando la llamada viene de una escritura del usuario (solo
     * interesa subir lo recien guardado, rapido); true en la sincronizacion de fondo del Worker.
     */
    suspend fun sincronizarAhora(incluirDescarga: Boolean = true): ResultadoSincronizacion {
        val uid = auth.usuarioActual()?.uid ?: return ResultadoSincronizacion.SIN_SESION
        if (!conectividad.hayConexion()) return ResultadoSincronizacion.SIN_CONEXION

        var huboError = false
        var sinConexion = false
        fun anotar(fallo: Fallo) {
            if (fallo == Fallo.SIN_CONEXION) sinConexion = true else if (fallo == Fallo.ERROR) huboError = true
        }

        // Categorias primero: productos y movimientos dependen de ellas. Un fallo en una parte
        // (p. ej. un error del servidor) no impide intentar las demas.
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

    // ---------- 0. Categorias pendientes (Room -> API PHP) ----------

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
                // Un solo POST para alta, edicion y cambio de estado: el servidor crea o actualiza por id.
                categoriasRemoto.guardarCategoria(entidad.toApiDto())
                categoriasLocal.marcarSincronizado(entidad.id, entidad.updatedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RemotoException.SinConexion) {
                Log.w(TAG, "Sin conexion subiendo la categoria ${entidad.id}; queda pendiente")
                return Fallo.SIN_CONEXION
            } catch (e: Exception) {
                // El registro local NO se toca: sigue pendiente y se reintentara.
                Log.e(TAG, "No se pudo subir la categoria ${entidad.id}", e)
                fallo = Fallo.ERROR
            }
        }
        return fallo
    }

    // ---------- A. Productos pendientes (Room -> API PHP) ----------

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
                    // POST: crea (o, si ya existiera por un reintento, actualiza via
                    // ON DUPLICATE KEY UPDATE en el backend).
                    EstadoSincronizacion.PENDIENTE_CREAR.name -> productosRemoto.crearProducto(producto.toApiDto(uid))
                    EstadoSincronizacion.PENDIENTE_ACTUALIZAR.name -> productosRemoto.actualizarProducto(entidad.id, producto.toApiDto(uid))
                    EstadoSincronizacion.PENDIENTE_ELIMINAR.name -> productosRemoto.eliminarProducto(entidad.id)
                    else -> continue
                }
                // La lapida local (deletedAt) NO se borra: conserva la relacion con los
                // movimientos historicos y evita que una descarga posterior la "resucite".
                productosLocal.marcarSincronizado(entidad.id, entidad.updatedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: RemotoException.SinConexion) {
                Log.w(TAG, "Sin conexion subiendo el producto ${entidad.id}; queda pendiente")
                return Fallo.SIN_CONEXION
            } catch (e: Exception) {
                // El registro local NO se toca: sigue pendiente y se reintentara.
                Log.e(TAG, "No se pudo subir el producto ${entidad.id}", e)
                fallo = Fallo.ERROR
            }
        }
        return fallo
    }

    // ---------- B. Movimientos pendientes (Room -> API PHP) ----------
    // A diferencia de productos, los movimientos son INMUTABLES una vez creados: el backend solo
    // expone "crear" (POST) y "listar" (GET), nunca actualizar/eliminar.

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
                // El registro local NO se toca: sigue pendiente y se reintentara.
                Log.e(TAG, "No se pudo subir el movimiento ${entidad.id}", e)
                fallo = Fallo.ERROR
            }
        }
        return fallo
    }

    // ---------- C0. Descarga de categorias (API PHP -> Room) ----------
    // Va antes que la descarga de productos: en Room cada producto tiene una clave foranea hacia su
    // categoria, asi que si la categoria (creada en otro dispositivo) no existe aqui todavia, el
    // producto no se podria guardar.

    private suspend fun descargarCategorias(): Fallo {
        var fallo = Fallo.NINGUNO
        try {
            for (remoto in categoriasRemoto.obtenerCategorias()) {
                if (remoto.id <= 0L) continue
                try {
                    val local = categoriasLocal.buscarPorId(remoto.id)
                    when {
                        // Nueva en el servidor: entra a Room ya marcada como SINCRONIZADA.
                        local == null -> categoriasLocal.insertarIgnorando(remoto.toEntity())
                        // Hay un cambio local sin subir: manda lo local, no se pisa.
                        local.estadoSincronizacion != EstadoSincronizacion.SINCRONIZADO.name -> Unit
                        // Gana el mas reciente: solo si el servidor es mas nuevo que la copia local.
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

    // ---------- C. Descarga de productos (API PHP -> Room) ----------

    private suspend fun descargarProductos(): Fallo {
        var fallo = Fallo.NINGUNO
        try {
            for (remoto in productosRemoto.obtenerProductos()) {
                if (remoto.id.isBlank()) continue
                try {
                    val local = productosLocal.obtenerPorIdIncluyendoEliminados(remoto.id)
                    val remotoDomain = remoto.toDomain()
                    when {
                        // Nuevo en el servidor: entra a Room ya marcado como SINCRONIZADO.
                        local == null -> productosLocal.insertarIgnorando(remotoDomain.toEntity(remoto.ownerUid))
                        // Hay un cambio local sin subir: manda lo local, no se pisa.
                        local.estadoSincronizacion != EstadoSincronizacion.SINCRONIZADO.name -> Unit
                        // Last write wins: solo si el servidor es mas reciente que la copia local.
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

    // ---------- D. Descarga de movimientos (API PHP -> Room) ----------
    // Al ser inmutables no hay "last write wins": si el movimiento ya existe localmente (por id,
    // sea cual sea su estado de sincronizacion) se ignora tal cual esta; si no existe todavia, se
    // inserta ya SINCRONIZADO. insertarIgnorando() (INSERT OR IGNORE) evita ademas cualquier
    // duplicado si dos sincronizaciones se solapan.

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
