package com.panchito.inventario.data.remote

import com.panchito.inventario.BuildConfig
import com.panchito.inventario.data.remote.api.CategoriaApiService
import com.panchito.inventario.data.remote.api.EmpleadoApiService
import com.panchito.inventario.data.remote.api.MovimientoApiService
import com.panchito.inventario.data.remote.api.ProductoApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Punto unico de configuracion de Retrofit.
 *
 * IMPORTANTE sobre la IP (localhost / 10.0.2.2 / IP local):
 * - "localhost" o "127.0.0.1" en Android SIEMPRE apunta al propio dispositivo/emulador, NUNCA
 *   a tu PC. Usarlo aca haria que la app buscara el servidor dentro de si misma: fallaria.
 * - "10.0.2.2" es una direccion especial que SOLO entiende el EMULADOR de Android Studio: la
 *   redirige automaticamente al "localhost" de la PC que lo hospeda. Sirve unicamente para el
 *   emulador, nunca para un celular fisico.
 * - Para un CELULAR FISICO conectado por WiFi a la MISMA red que tu PC, hay que reemplazar
 *   10.0.2.2 por la IP local de tu PC en esa red (ej. 192.168.1.35, se obtiene con "ipconfig"
 *   en Windows -> buscar "Direccion IPv4" del adaptador WiFi). Esa misma IP hay que agregarla
 *   tambien en res/xml/network_security_config.xml.
 *
 * El puerto: XAMPP suele usar el 80 para Apache (no confundir con el 3307 que configuraste para
 * MySQL en config.php: son servicios distintos). Si tu Apache corre en otro puerto, agregalo
 * despues del host, ej. "http://10.0.2.2:8080/inventario_api/".
 */
object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2/inventario_api/"

    private const val TIMEOUT_SEGUNDOS = 15L

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // BODY (no solo BASIC): util para ver en Logcat el JSON exacto que va y
                    // viene mientras probas la integracion nueva con tu API PHP.
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val productoApiService: ProductoApiService by lazy { retrofit.create(ProductoApiService::class.java) }

    val categoriaApiService: CategoriaApiService by lazy { retrofit.create(CategoriaApiService::class.java) }

    val empleadoApiService: EmpleadoApiService by lazy { retrofit.create(EmpleadoApiService::class.java) }

    val movimientoApiService: MovimientoApiService by lazy { retrofit.create(MovimientoApiService::class.java) }
}