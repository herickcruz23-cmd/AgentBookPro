package com.agentbook.pro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentbook.pro.AppConfig
import com.agentbook.pro.data.model.AppointmentInsert
import com.agentbook.pro.data.model.AppointmentStats
import com.agentbook.pro.data.model.ChatMessage
import com.agentbook.pro.data.model.ChatUiMessage
import com.agentbook.pro.data.model.ServicePricing
import com.agentbook.pro.data.repository.Repository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: Repository = Repository()
) : ViewModel() {

    private val errorHandler = CoroutineExceptionHandler { _, e ->
        _messages.update {
            it + ChatUiMessage(
                role = ChatUiMessage.Role.ASSISTANT,
                text = "Error inesperado: ${e.message ?: e.javaClass.simpleName}"
            )
        }
        _isSending.value = false
    }

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(
        listOf(
            ChatUiMessage(
                role = ChatUiMessage.Role.ASSISTANT,
                text = "¡Hola! Soy AgentBook 🦷. Te ayudo a agendar tu cita dental en menos de un minuto. ¿Cuál es tu nombre completo?"
            )
        )
    )
    val messages: StateFlow<List<ChatUiMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _stats = MutableStateFlow(AppointmentStats())
    val stats: StateFlow<AppointmentStats> = _stats.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        refreshStats()
    }

    fun onInputChanged(value: String) {
        _input.value = value
    }

    fun consumeToast() {
        _toast.value = null
    }

    fun send() {
        val text = _input.value.trim()
        if (text.isEmpty() || _isSending.value) return

        _input.value = ""
        val userMsg = ChatUiMessage(role = ChatUiMessage.Role.USER, text = text)
        val loadingMsg = ChatUiMessage(
            role = ChatUiMessage.Role.ASSISTANT,
            text = "Pensando…",
            isLoading = true
        )
        _messages.update { it + userMsg + loadingMsg }
        _isSending.value = true

        viewModelScope.launch(errorHandler) {
            val history = buildHistory()
            val result = repo.chat(history)
            _messages.update { current -> current.filterNot { it.id == loadingMsg.id } }

            result.onSuccess { reply ->
                val (visible, booking) = extractBookingBlock(reply)
                if (booking != null) {
                    val costo = ServicePricing.calculate(booking.servicio)
                    val bookingWithCost = booking.copy(costo = costo)
                    val costMsg = "💰 Costo total: <b>${ServicePricing.format(costo)}</b>"
                        .replace("<b>", "").replace("</b>", "")
                    if (visible.isNotEmpty()) {
                        _messages.update {
                            it + ChatUiMessage(
                                role = ChatUiMessage.Role.ASSISTANT,
                                text = "$visible\n\n$costMsg"
                            )
                        }
                    } else {
                        _messages.update {
                            it + ChatUiMessage(role = ChatUiMessage.Role.ASSISTANT, text = costMsg)
                        }
                    }
                    persistAppointment(bookingWithCost)
                } else if (visible.isNotEmpty()) {
                    _messages.update {
                        it + ChatUiMessage(role = ChatUiMessage.Role.ASSISTANT, text = visible)
                    }
                }
            }.onFailure { e ->
                _messages.update {
                    it + ChatUiMessage(
                        role = ChatUiMessage.Role.ASSISTANT,
                        text = "Error: ${e.message ?: "no se pudo conectar con la IA"}"
                    )
                }
            }
            _isSending.value = false
        }
    }

    private fun buildHistory(): List<ChatMessage> {
        val system = ChatMessage(role = "system", content = AppConfig.SYSTEM_PROMPT.trimIndent())
        val turns = _messages.value
            .filterNot { it.isLoading }
            .filter { it.role == ChatUiMessage.Role.USER || it.role == ChatUiMessage.Role.ASSISTANT }
            .map {
                ChatMessage(
                    role = if (it.role == ChatUiMessage.Role.USER) "user" else "assistant",
                    content = it.text
                )
            }
        return listOf(system) + turns
    }

    private fun extractBookingBlock(raw: String): Pair<String, AppointmentInsert?> {
        val regex = Regex("<BOOK>(.*?)</BOOK>", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(raw) ?: return raw to null
        val payload = match.groupValues[1]
        val map = payload.split(";")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim().lowercase() to parts[1].trim() else null
            }
            .toMap()
        val insert = runCatching {
            AppointmentInsert(
                nombre = map.getValue("nombre"),
                telefono = map.getValue("telefono"),
                fecha = map.getValue("fecha"),
                hora = map.getValue("hora"),
                servicio = map.getValue("servicio")
            )
        }.getOrNull()
        val visible = raw.replace(regex, "").trim()
        return visible to insert
    }

    private fun persistAppointment(insert: AppointmentInsert) {
        viewModelScope.launch(errorHandler) {
            // 1) Guardar en base de datos
            val dbResult = repo.createAppointment(insert)
            dbResult.onSuccess {
                refreshStats()
            }.onFailure { e ->
                _messages.update {
                    it + ChatUiMessage(
                        role = ChatUiMessage.Role.ASSISTANT,
                        text = "⚠️ No se pudo guardar en la base de datos: ${e.message}"
                    )
                }
            }

            // 2) Enviar notificación a Telegram (independiente de la BD)
            val tgResult = repo.sendTelegramNotification(insert)
            tgResult.onSuccess {
                _toast.value = "📲 Notificación enviada a Telegram ✓"
            }.onFailure { e ->
                val msg = e.message ?: "error desconocido"
                if (msg.contains("no configurado")) {
                    _messages.update {
                        it + ChatUiMessage(
                            role = ChatUiMessage.Role.ASSISTANT,
                            text = "ℹ️ Telegram no está configurado (revisa TELEGRAM_BOT_TOKEN y TELEGRAM_CHAT_ID en AppConfig.kt)."
                        )
                    }
                } else {
                    _messages.update {
                        it + ChatUiMessage(
                            role = ChatUiMessage.Role.ASSISTANT,
                            text = "⚠️ Telegram falló: $msg"
                        )
                    }
                }
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch(errorHandler) {
            repo.getStats().onSuccess { _stats.value = it }
        }
    }
}