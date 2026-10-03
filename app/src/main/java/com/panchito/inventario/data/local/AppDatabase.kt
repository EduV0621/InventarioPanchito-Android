package com.panchito.inventario.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.panchito.inventario.data.local.dao.CategoriaDao
import com.panchito.inventario.data.local.dao.EmpleadoDao
import com.panchito.inventario.data.local.dao.MovimientoDao
import com.panchito.inventario.data.local.dao.ProductoDao
import com.panchito.inventario.data.local.entity.CategoriaEntity
import com.panchito.inventario.data.local.entity.EmpleadoEntity
import com.panchito.inventario.data.local.entity.MovimientoEntity
import com.panchito.inventario.data.local.entity.ProductoEntity

@Database(
    entities = [ProductoEntity::class, CategoriaEntity::class, EmpleadoEntity::class, MovimientoEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun empleadoDao(): EmpleadoDao
    abstract fun movimientoDao(): MovimientoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val categoriasIniciales = listOf(
            "Abarrotes",
            "Bebidas",
            "Lácteos y refrigerados",
            "Limpieza",
            "Otros",
            "Panadería"
        )
        private val callbackCategoriasIniciales = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                categoriasIniciales.forEach { nombre ->

                    db.execSQL(
                        "INSERT INTO categorias (nombre, activa, estadoSincronizacion, updatedAt) " +
                            "VALUES ('$nombre', 1, 'SINCRONIZADO', 0)"
                    )
                }
            }
        }

        private fun migracion1a2(uidParaDatosAntiguos: String) = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val uid = uidParaDatosAntiguos.replace("'", "")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS productos_nueva (
                        id TEXT NOT NULL,
                        ownerUid TEXT NOT NULL,
                        codigo TEXT NOT NULL,
                        nombre TEXT NOT NULL,
                        categoriaId INTEGER NOT NULL,
                        precio REAL NOT NULL,
                        stock INTEGER NOT NULL,
                        stockMinimo INTEGER NOT NULL,
                        fechaVencimientoMillis INTEGER,
                        activo INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(categoriaId) REFERENCES categorias(id) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO productos_nueva (id, ownerUid, codigo, nombre, categoriaId, precio, stock, stockMinimo, fechaVencimientoMillis, activo)
                    SELECT CAST(id AS TEXT), '$uid', codigo, nombre, categoriaId, precio, stock, stockMinimo, fechaVencimientoMillis, activo FROM productos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE productos")
                db.execSQL("ALTER TABLE productos_nueva RENAME TO productos")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_productos_ownerUid_codigo ON productos (ownerUid, codigo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_categoriaId ON productos (categoriaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_ownerUid ON productos (ownerUid)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS movimientos_nueva (
                        id INTEGER NOT NULL,
                        productoId TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        cantidad INTEGER NOT NULL,
                        motivo TEXT NOT NULL,
                        usuarioId INTEGER NOT NULL,
                        fechaHoraMillis INTEGER NOT NULL,
                        estadoSincronizacion TEXT NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(productoId) REFERENCES productos(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
                        FOREIGN KEY(usuarioId) REFERENCES empleados(id) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO movimientos_nueva (id, productoId, tipo, cantidad, motivo, usuarioId, fechaHoraMillis, estadoSincronizacion)
                    SELECT id, CAST(productoId AS TEXT), tipo, cantidad, motivo, usuarioId, fechaHoraMillis, estadoSincronizacion FROM movimientos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE movimientos")
                db.execSQL("ALTER TABLE movimientos_nueva RENAME TO movimientos")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_productoId ON movimientos (productoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_usuarioId ON movimientos (usuarioId)")
            }
        }

        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS movimientos_nueva (
                        id TEXT NOT NULL,
                        productoId TEXT NOT NULL,
                        ownerUid TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        cantidad INTEGER NOT NULL,
                        motivo TEXT NOT NULL,
                        fechaHoraMillis INTEGER NOT NULL,
                        estadoSincronizacion TEXT NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(productoId) REFERENCES productos(id) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO movimientos_nueva (id, productoId, ownerUid, tipo, cantidad, motivo, fechaHoraMillis, estadoSincronizacion)
                    SELECT CAST(m.id AS TEXT), m.productoId,
                           COALESCE((SELECT p.ownerUid FROM productos p WHERE p.id = m.productoId), ''),
                           m.tipo, m.cantidad, m.motivo, m.fechaHoraMillis, m.estadoSincronizacion
                    FROM movimientos m
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE movimientos")
                db.execSQL("ALTER TABLE movimientos_nueva RENAME TO movimientos")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_productoId ON movimientos (productoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_ownerUid ON movimientos (ownerUid)")
            }
        }

        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val ahora = System.currentTimeMillis()

                db.execSQL("ALTER TABLE productos ADD COLUMN estadoSincronizacion TEXT NOT NULL DEFAULT 'SINCRONIZADO'")
                db.execSQL("ALTER TABLE productos ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE productos ADD COLUMN deletedAt INTEGER")
                db.execSQL("UPDATE productos SET updatedAt = $ahora WHERE updatedAt = 0")

                db.execSQL("ALTER TABLE movimientos ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE movimientos SET updatedAt = fechaHoraMillis WHERE updatedAt = 0")
                db.execSQL("UPDATE movimientos SET estadoSincronizacion = 'PENDIENTE_CREAR' WHERE estadoSincronizacion = 'PENDIENTE'")

                db.execSQL("DROP INDEX IF EXISTS index_productos_ownerUid_codigo")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_ownerUid_codigo ON productos (ownerUid, codigo)")
            }
        }

        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val ahora = System.currentTimeMillis()
                db.execSQL("ALTER TABLE categorias ADD COLUMN estadoSincronizacion TEXT NOT NULL DEFAULT 'SINCRONIZADO'")
                db.execSQL("ALTER TABLE categorias ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE categorias SET estadoSincronizacion = 'PENDIENTE_CREAR', updatedAt = $ahora")
            }
        }

        fun getInstance(context: Context, uidActual: () -> String? = { null }): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventario_panchito.db"
                )
                    .addCallback(callbackCategoriasIniciales)
                    .addMigrations(migracion1a2(uidActual().orEmpty()), MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
