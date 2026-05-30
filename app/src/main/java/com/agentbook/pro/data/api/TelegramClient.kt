package com.agentbook.pro.data.api

import com.agentbook.pro.AppConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object TelegramClient {

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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: TelegramAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TelegramAPI::class.java)
    }

    val isConfigured: Boolean
        get() = AppConfig.TELEGRAM_BOT_TOKEN.isNotBlank() &&
                AppConfig.TELEGRAM_CHAT_ID.isNotBlank()

    /** URL completa de sendMessage (incluye token sin codificar). */
    val sendMessageUrl: String
        get() = "https://api.telegram.org/bot${AppConfig.TELEGRAM_BOT_TOKEN}/sendMessage"

    val chatId: String get() = AppConfig.TELEGRAM_CHAT_ID
}
