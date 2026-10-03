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
