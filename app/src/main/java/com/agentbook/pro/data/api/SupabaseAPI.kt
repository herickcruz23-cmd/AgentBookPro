package com.agentbook.pro.data.api

import com.agentbook.pro.data.model.Appointment
import com.agentbook.pro.data.model.AppointmentInsert
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseAPI {

    @Headers(
        "Content-Type: application/json",
        "Prefer: return=representation"
    )
    @POST("rest/v1/appointments")
    suspend fun createAppointment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body appointment: AppointmentInsert
    ): Response<List<Appointment>>

    @GET("rest/v1/appointments")
    suspend fun getAllAppointments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "fecha.desc,hora.desc"
    ): Response<List<Appointment>>

    @GET("rest/v1/appointments")
    suspend fun getAppointmentsByDate(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("fecha") fechaEq: String
    ): Response<List<Appointment>>
}
