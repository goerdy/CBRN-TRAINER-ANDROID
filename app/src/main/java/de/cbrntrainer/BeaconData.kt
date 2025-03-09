package de.cbrntrainer

data class BeaconData(
    val address: String,
    val name: String?,
    val rssi: Int,
    val isSaved: Boolean = false,
    val type: String = "radiation",
    val rate: String = "0.0"  // mSv/h als String für alfanumerische Eingabe
) 