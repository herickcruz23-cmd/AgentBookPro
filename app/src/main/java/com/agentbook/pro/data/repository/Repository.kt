package com.agentbook.pro.data.repository

import com.agentbook.pro.AppConfig
import com.agentbook.pro.data.api.RetrofitClient
import com.agentbook.pro.data.api.SupabaseClient
import com.agentbook.pro.data.api.TelegramClient
import com.agentbook.pro.data.api.TelegramMessage
import com.agentbook.pro.data.model.Appointment
import com.agentbook.pro.data.model.AppointmentInsert
import com.agentbook.pro.data.model.AppointmentStats
import com.agentbook.pro.data.model.ChatMessage
import com.agentbook.pro.data.model.ChatRequest
import com.agentbook.pro.data.model.ServicePricing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Repository {

    private val chatApi by lazy { RetrofitClient.chatApi }
    private val supabase by lazy { SupabaseClient.api }
    private val telegram by lazy { TelegramClient.api }

    suspend fun chat(history: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = AppConfig.API_KEY
            val expected = AppConfig.expectedKeyPrefix
            if (key.isBlank()) {
                error("Falta la API_KEY. Edita HARDCODED_API_KEY en AppConfig.kt y haz Rebuild.")
            }
            if (!key.startsWith(expected, ignoreCase = true)) {
                error("API_KEY inválida: debería empezar con '$expected'. Revisa AppConfig.kt.")
            }
            val request = ChatRequest(
                model = AppConfig.MODEL,
                messages = history
            )
            val resp = chatApi.chat(request)
            if (!resp.isSuccessful) {
                val body = resp.errorBody()?.string().orEmpty()
                val hint = when (resp.code()) {
                    401 -> "\n\n👉 La clave es inválida. Verifica en " +
                            (if (AppConfig.expectedKeyPrefix == "gsk_") "https://console.groq.com/keys"
                            else "https://platform.openai.com/api-keys")
                    429 -> "\n\n👉 Excediste el rate limit. Espera unos segundos."
                    else -> ""
                }
                error("${AppConfig.PROVIDER_NAME} ${resp.code()}: ${body.take(300)}$hint")
            }
            val body = resp.body() ?: error("Respuesta vacía")
            body.choices.firstOrNull()?.message?.content?.trim()
                ?: error("Sin contenido en la respuesta")
        }
    }

    suspend fun createAppointment(insert: AppointmentInsert): Result<Appointment> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!SupabaseClient.isConfigured) {
                    error("Configura SUPABASE_URL y SUPABASE_ANON_KEY en AppConfig.kt")
                }
                // Hasta 3 intentos: el proyecto Supabase free puede estar "despertando".
                var lastError: Throwable? = null
                repeat(3) { attempt ->
                    try {
                        val resp = supabase.createAppointment(
                            apiKey = SupabaseClient.apiKey,
                            authorization = SupabaseClient.authHeader,
                            appointment = insert
                        )
                        if (resp.isSuccessful) {
                            return@runCatching resp.body()?.firstOrNull()
                                ?: error("Supabase no devolvió la cita creada")
                        }
                        val body = resp.errorBody()?.string().orEmpty()
                        val hint = if (body.contains("PGRST204")) {
                            "\n\n👉 Tu tabla 'appointments' no tiene las columnas correctas. " +
                                    "Abre el SQL Editor en Supabase y ejecuta TODO el supabase.sql."
                        } else ""
                        // Errores 4xx (salvo 408/429) no se reintentan: son de configuración.
                        if (resp.code() in 400..499 && resp.code() != 408 && resp.code() != 429) {
                            error("Supabase ${resp.code()}: ${body.take(300)}$hint")
                        }
                        lastError = IllegalStateException("Supabase ${resp.code()}: ${body.take(200)}")
                    } catch (e: Exception) {
                        lastError = e
                    }
                    if (attempt < 2) delay(2000) // espera 2s antes de reintentar
                }
                throw lastError ?: IllegalStateException("Supabase: no se pudo guardar (timeout). " +
                        "Verifica tu internet o que el proyecto Supabase esté activo.")
            }
        }

    suspend fun getStats(): Result<AppointmentStats> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseClient.isConfigured) {
                return@runCatching AppointmentStats()
            }
            val all = supabase.getAllAppointments(
                apiKey = SupabaseClient.apiKey,
                authorization = SupabaseClient.authHeader
            )
            if (!all.isSuccessful) {
                error("Supabase ${all.code()}: ${all.errorBody()?.string().orEmpty().take(200)}")
            }
            val list = all.body().orEmpty()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayCount = list.count { it.fecha == today }
            AppointmentStats(total = list.size, today = todayCount)
        }
    }

    suspend fun sendTelegramNotification(appointment: AppointmentInsert): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!TelegramClient.isConfigured) {
                    error("Telegram no configurado (TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID).")
                }
                val costo = appointment.costo ?: ServicePricing.calculate(appointment.servicio)

                // Intento 1: con formato HTML
                val htmlText = buildTelegramMessage(appointment, costo, html = true)
                var resp = telegram.sendMessage(
                    url = TelegramClient.sendMessageUrl,
                    body = TelegramMessage(
                        chat_id = TelegramClient.chatId,
                        text = htmlText,
                        parse_mode = "HTML"
                    )
                )
                var body = resp.body()

                // Si el HTML rompió el parseo, reintenta en texto plano (sin parse_mode)
                if (!resp.isSuccessful || body?.ok != true) {
                    val plainText = buildTelegramMessage(appointment, costo, html = false)
                    resp = telegram.sendMessage(
                        url = TelegramClient.sendMessageUrl,
                        body = TelegramMessage(
                            chat_id = TelegramClient.chatId,
                            text = plainText,
                            parse_mode = ""
                        )
                    )
                    body = resp.body()
                }

                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string().orEmpty().take(250)
                    val hint = when (resp.code()) {
                        400 -> "\n👉 Revisa que el CHAT_ID sea correcto y que YA le hayas escrito a tu bot."
                        401 -> "\n👉 El TOKEN del bot es inválido. Revisa @BotFather."
                        403 -> "\n👉 Tu bot está bloqueado o nunca le escribiste. Manda /start a tu bot."
                        404 -> "\n👉 Token mal escrito (URL del bot no existe)."
                        else -> ""
                    }
                    error("Telegram ${resp.code()}: $err$hint")
                }
                if (body?.ok != true) {
                    error("Telegram rechazó el mensaje: ${body?.description ?: "desconocido"}")
                }
            }
        }

    private fun buildTelegramMessage(a: AppointmentInsert, costo: Double, html: Boolean): String {
        val costoFmt = ServicePricing.format(costo)
        return if (html) {
            buildString {
                append("🚨 <b>NUEVA CITA AGENDADA</b> 🚨\n\n")
                append("🏥 <b>").append(escape(AppConfig.BUSINESS_NAME)).append("</b>\n\n")
                append("👤 <b>Paciente:</b> ").append(escape(a.nombre)).append("\n")
                append("📱 <b>WhatsApp:</b> <code>").append(escape(a.telefono)).append("</code>\n")
                append("🦷 <b>Servicio:</b> ").append(escape(a.servicio)).append("\n")
                append("📅 <b>Fecha:</b> ").append(escape(a.fecha)).append("\n")
                append("🕐 <b>Hora:</b> ").append(escape(a.hora)).append("\n\n")
                append("💰 <b>Costo total: ").append(escape(costoFmt)).append("</b>\n\n")
                append("✅ <i>Procesado por AgentBook IA</i>")
            }
        } else {
            // Texto plano (sin etiquetas) como respaldo a prueba de fallos
            buildString {
                append("🚨 NUEVA CITA AGENDADA 🚨\n\n")
                append("🏥 ").append(AppConfig.BUSINESS_NAME).append("\n\n")
                append("👤 Paciente: ").append(a.nombre).append("\n")
                append("📱 WhatsApp: ").append(a.telefono).append("\n")
                append("🦷 Servicio: ").append(a.servicio).append("\n")
                append("📅 Fecha: ").append(a.fecha).append("\n")
                append("🕐 Hora: ").append(a.hora).append("\n\n")
                append("💰 Costo total: ").append(costoFmt).append("\n\n")
                append("✅ Procesado por AgentBook IA")
            }
        }
    }

    /** Escape HTML para que Telegram no rompa por '<', '>' o '&'. */
    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}