package com.agentbook.pro

/**
 * ============================================================
 *  CONFIGURACIÓN DE CLAVES — AgentBook
 * ============================================================
 *  IA: Groq (Llama 3.3 70B) — https://console.groq.com/keys
 *  BD: Supabase — https://supabase.com
 *  Notificaciones: Telegram Bot (vía @BotFather)
 *
 *  Para configurar:
 *  1. Rellena los HARDCODED_* abajo, O
 *  2. Crea keys.properties en la raíz del proyecto
 * ============================================================
 */
object AppConfig {

    enum class Provider { GROQ, OPENAI }

    // ⬇⬇⬇ EDITA SOLO ESTAS LÍNEAS ⬇⬇⬇

    private val PROVIDER = Provider.GROQ

    // --- IA (Groq: "gsk_...", OpenAI: "sk-...") ---
    private const val HARDCODED_API_KEY = ""

    // --- Supabase ---
    // URL: SOLO el dominio raíz, SIN /rest/v1/
    // Ej: "https://tuproyecto.supabase.co"
    private const val HARDCODED_SUPABASE_URL = ""
    private const val HARDCODED_SUPABASE_KEY = ""

    // --- Telegram Bot ---
    private const val HARDCODED_TELEGRAM_BOT_TOKEN = ""
    private const val HARDCODED_TELEGRAM_CHAT_ID = ""

    // Nombre del negocio (sale en la notificación de Telegram)
    const val BUSINESS_NAME = "Clínica Dental AgentBook"

    // ⬆⬆⬆ ------------------------------------------- ⬆⬆⬆

    // --- Detección de placeholders ---
    private val PLACEHOLDER_MARKERS = listOf("PEGA", "AQUI", "TU_CLAVE", "tu-clave", "pon-aqui", "TU_TOKEN", "TU_CHAT")

    private fun String.isRealValue(): Boolean {
        val v = trim()
        if (v.isBlank()) return false
        return PLACEHOLDER_MARKERS.none { v.contains(it, ignoreCase = true) }
    }

    private fun resolve(fromBuild: String, fromCode: String): String {
        // El valor escrito en AppConfig.kt (fromCode) tiene PRIORIDAD.
        // Así evitamos que un keys.properties viejo pise tu configuración.
        if (fromCode.isRealValue()) return fromCode.trim()
        if (fromBuild.isRealValue()) return fromBuild.trim()
        return ""
    }

    // --- Endpoints ---
    val BASE_URL: String
        get() = when (PROVIDER) {
            Provider.GROQ -> "https://api.groq.com/openai/"
            Provider.OPENAI -> "https://api.openai.com/"
        }

    val MODEL: String
        get() = when (PROVIDER) {
            Provider.GROQ -> "llama-3.3-70b-versatile"
            Provider.OPENAI -> "gpt-4o-mini"
        }

    val PROVIDER_NAME: String
        get() = when (PROVIDER) {
            Provider.GROQ -> "Groq · Llama 3.3 70B"
            Provider.OPENAI -> "OpenAI · gpt-4o-mini"
        }

    val API_KEY: String
        get() = resolve(BuildConfig.API_KEY, HARDCODED_API_KEY)

    val SUPABASE_URL: String
        get() {
            val raw = resolve(BuildConfig.SUPABASE_URL, HARDCODED_SUPABASE_URL)
            if (raw.isBlank()) return ""
            return raw
                .removeSuffix("/")
                .removeSuffix("/rest/v1")
                .removeSuffix("/rest")
        }

    val SUPABASE_ANON_KEY: String
        get() = resolve(BuildConfig.SUPABASE_ANON_KEY, HARDCODED_SUPABASE_KEY)

    val TELEGRAM_BOT_TOKEN: String
        get() = resolve(BuildConfig.TELEGRAM_BOT_TOKEN, HARDCODED_TELEGRAM_BOT_TOKEN)

    val TELEGRAM_CHAT_ID: String
        get() = resolve(BuildConfig.TELEGRAM_CHAT_ID, HARDCODED_TELEGRAM_CHAT_ID)

    val expectedKeyPrefix: String
        get() = when (PROVIDER) {
            Provider.GROQ -> "gsk_"
            Provider.OPENAI -> "sk-"
        }

    /** Diagnóstico: muestra qué Supabase está realmente activo (sin exponer la key completa). */
    val supabaseDebug: String
        get() {
            val url = SUPABASE_URL
            val key = SUPABASE_ANON_KEY
            val proj = url.removePrefix("https://").substringBefore(".supabase").take(8)
            val keyTail = if (key.length > 6) "…${key.takeLast(6)}" else "(vacía)"
            return "DB:$proj key:$keyTail"
        }

    const val SYSTEM_PROMPT = """
Eres AgentBook, un asistente IA especializado en agendar citas dentales.
Tu objetivo es ayudar a los pacientes a programar citas de forma clara, breve y amable.

Reglas:
- Pregunta de forma ordenada por: nombre completo, teléfono, servicio (limpieza, blanqueamiento, ortodoncia, extracción, revisión general, urgencia), fecha (YYYY-MM-DD) y hora (HH:MM en formato 24h).
- Solo pide UN dato a la vez.
- Confirma cada dato antes de pasar al siguiente.
- Cuando tengas TODOS los datos, responde con un bloque exacto delimitado así (sin comillas ni texto extra):
  <BOOK>nombre=...;telefono=...;fecha=YYYY-MM-DD;hora=HH:MM;servicio=...</BOOK>
- Después del bloque, agrega un mensaje de confirmación cálido al paciente.
- Horario válido: lunes a sábado, 09:00 a 19:00.
- Si el paciente pide algo fuera de citas dentales, redirígelo amablemente.
"""
}
