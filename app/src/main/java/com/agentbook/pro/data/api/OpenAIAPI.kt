package com.agentbook.pro.data.api

import com.agentbook.pro.data.model.ChatRequest
import com.agentbook.pro.data.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAIAPI {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}
