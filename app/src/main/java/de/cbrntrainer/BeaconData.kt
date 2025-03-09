package de.cbrntrainer

data class BeaconData(
    val address: String,
    val name: String?,
    val rssi: Int,
    val type: String = "Strahler",
    val rate: String = "5",  // Standardwert auf 5 gesetzt
    val isSaved: Boolean = false
) 