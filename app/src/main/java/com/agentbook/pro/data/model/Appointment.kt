package com.agentbook.pro.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: Long? = null,
    val nombre: String,
    val telefono: String,
    val fecha: String,
    val hora: String,
    val servicio: String,
    val costo: Double? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AppointmentInsert(
    val nombre: String,
    val telefono: String,
    val fecha: String,
    val hora: String,
    val servicio: String,
    val costo: Double? = null
)

data class ChatUiMessage(
    val id: Long = System.nanoTime(),
    val role: Role,
    val text: String,
    val isLoading: Boolean = false
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
}

data class AppointmentStats(
    val total: Int = 0,
    val today: Int = 0
)

/** Precios por servicio en MXN (puedes ajustarlos). */
object ServicePricing {
    private val prices = mapOf(
        "limpieza" to 600.0,
        "blanqueamiento" to 2500.0,
        "ortodoncia" to 8000.0,
        "extracción" to 900.0,
        "extraccion" to 900.0,
        "revisión general" to 350.0,
        "revision general" to 350.0,
        "revisión" to 350.0,
        "revision" to 350.0,
        "urgencia" to 1200.0,
        "endodoncia" to 3500.0,
        "implante" to 15000.0,
        "resina" to 700.0,
        "corona" to 6000.0
    )

    /** Calcula costo total. El servicio puede contener múltiples palabras clave. */
    fun calculate(servicio: String): Double {
        val lower = servicio.lowercase()
        val matched = prices.entries
            .filter { lower.contains(it.key) }
            // evita doble cobro por "revisión" + "revisión general"
            .groupBy { it.value }
            .map { (price, _) -> price }
        return if (matched.isEmpty()) 500.0 else matched.sum()
    }

    fun format(amount: Double): String =
        "$" + String.format(java.util.Locale.US, "%,.2f", amount) + " MXN"
}
