package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.aladhan.com/v1/"
    private const val QURAN_BASE_URL = "https://api.alquran.cloud/v1/"

    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/v1/"
    private const val OVERPASS_BASE_URL = "https://overpass-api.de/api/"
    private const val EVP_CONTENT_BASE_URL = "https://service-content.ezanvaktipro.com/"

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val overpassOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(22, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "NamazVaktiApp/1.0 (Android; Turkish Prayer & Mosque Finder App)")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val weatherApiService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApiService::class.java)
    }

    val apiService: AladhanApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AladhanApiService::class.java)
    }

    val quranApiService: AlQuranApiService by lazy {
        Retrofit.Builder()
            .baseUrl(QURAN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlQuranApiService::class.java)
    }

    val overpassService: OverpassService by lazy {
        Retrofit.Builder()
            .baseUrl(OVERPASS_BASE_URL)
            .client(overpassOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OverpassService::class.java)
    }

    val evpMosqueService: EvpMosqueService by lazy {
        Retrofit.Builder()
            .baseUrl(EVP_CONTENT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EvpMosqueService::class.java)
    }

    val mosqueRepository: MosqueRepository by lazy {
        val fallback1 = Retrofit.Builder()
            .baseUrl("https://overpass.private.coffee/api/")
            .client(overpassOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build().create(OverpassService::class.java)
        val fallback2 = Retrofit.Builder()
            .baseUrl("https://maps.mail.ru/osm/tools/overpass/api/")
            .client(overpassOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build().create(OverpassService::class.java)
        MosqueRepository(
            service = overpassService,
            fallback = fallback1,
            extraFallbacks = listOf(fallback2),
            evpService = evpMosqueService
        )
    }
}
