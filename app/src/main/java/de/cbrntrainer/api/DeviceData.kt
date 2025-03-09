package de.cbrntrainer.api

import com.google.gson.annotations.SerializedName

data class DeviceData(
    @SerializedName("timestamp") val timestamp: Long = 0,
    @SerializedName("value") val value: Double = 0.0,
    @SerializedName("unit") val unit: String = "",
    @SerializedName("device_type") val deviceType: String = "",
    @SerializedName("status") val status: String = ""
)

data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("data") val data: MeasurementData
)

data class MeasurementData(
    @SerializedName("sessionID") val sessionId: String,
    @SerializedName("dosisleistung") val dosisleistung: Double,
    @SerializedName("dosis") val dosis: Double,
    @SerializedName("co") val co: Double,
    @SerializedName("ch4") val ch4: Double,
    @SerializedName("co2") val co2: Double,
    @SerializedName("o2") val o2: Double,
    @SerializedName("ibut") val ibut: Double,
    @SerializedName("nona") val nona: Double,
    @SerializedName("h2s") val h2s: Double,
    @SerializedName("nh3") val nh3: Double,
    @SerializedName("distance") val distance: Double,
    @SerializedName("source_strength") val sourceStrength: Double,
    @SerializedName("teletector") val teletector: Boolean,
    @SerializedName("cover") val cover: Boolean
) 