package com.agentbook.pro.data.api

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface TelegramAPI {
    // Usamos @Url con la URL completa para que Retrofit NO codifique
    // los caracteres ':' y '/' del token del bot.
    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Body body: TelegramMessage
    ): Response<TelegramResponse>
}

@Serializable
data class TelegramMessage(
    val chat_id: String,
    val text: String,
    val parse_mode: String = "HTML",
    val disable_web_page_preview: Boolean = true
)

@Serializable
data class TelegramResponse(
    val ok: Boolean = false,
    val description: String? = null
)
