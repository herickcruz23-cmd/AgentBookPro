package com.agentbook.pro.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 600
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: ChatMessage
)

@Serializable
data class OpenAIError(
    val error: OpenAIErrorDetail? = null
)

@Serializable
data class OpenAIErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
