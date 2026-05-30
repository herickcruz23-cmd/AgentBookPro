package com.agentbook.pro.data.api

import com.agentbook.pro.AppConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private const val FALLBACK_URL = "https://example.supabase.co/"

    /** True solo cuando hay credenciales reales configuradas. */
    val isConfigured: Boolean
        get() = AppConfig.SUPABASE_URL.startsWith("http", ignoreCase = true) &&
                AppConfig.SUPABASE_ANON_KEY.isNotBlank()

    private fun buildBaseUrl(): String {
        val raw = AppConfig.SUPABASE_URL.trim()
        if (!raw.startsWith("http", ignoreCase = true)) return FALLBACK_URL
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    val api: SupabaseAPI by lazy {
        Retrofit.Builder()
            .baseUrl(buildBaseUrl())
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SupabaseAPI::class.java)
    }

    val apiKey: String get() = AppConfig.SUPABASE_ANON_KEY
    val authHeader: String get() = "Bearer ${AppConfig.SUPABASE_ANON_KEY}"
}
