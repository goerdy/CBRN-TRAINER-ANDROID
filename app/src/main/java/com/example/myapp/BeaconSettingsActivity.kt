package com.example.myapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BeaconSettingsActivity : BaseActivity() {
    
    private lateinit var typeSpinner: Spinner
    private lateinit var rateInput: EditText
    private lateinit var beaconInfoText: TextView
    private lateinit var calibrationStatusText: TextView
    
    private var beaconAddress: String? = null
    private var beaconName: String? = null
    private var currentBeacon: BeaconData? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon_settings)
        
        beaconAddress = intent.getStringExtra("BEACON_ADDRESS")
        beaconName = intent.getStringExtra("BEACON_NAME")
        
        typeSpinner = findViewById(R.id.typeSpinner)
        rateInput = findViewById(R.id.rateInput)
        beaconInfoText = findViewById(R.id.beaconInfoText)
        calibrationStatusText = findViewById(R.id.calibrationStatusText)
        
        // Spinner mit Typen füllen
        ArrayAdapter.createFromResource(
            this,
            R.array.beacon_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = adapter
        }
        
        // Lade aktuelle Beacon-Daten
        loadBeaconData()
        
        // Kalibrieren Button
        findViewById<Button>(R.id.calibrateButton).setOnClickListener {
            val intent = Intent(this, CalibrationActivity::class.java)
            intent.putExtra("BEACON_ADDRESS", beaconAddress)
            intent.putExtra("BEACON_NAME", beaconName)
            startActivity(intent)
        }
        
        // Speichern Button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveBeaconSettings()
        }
        
        // Zurück Button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    private fun loadBeaconData() {
        beaconInfoText.text = "Beacon: ${beaconName ?: "Unbekannt"}\nAdresse: $beaconAddress"
        
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("saved_beacons", null)
        
        if (json != null) {
            val type = object : TypeToken<List<BeaconData>>() {}.type
            val beacons: List<BeaconData> = gson.fromJson(json, type)
            currentBeacon = beacons.find { it.address == beaconAddress }
            
            currentBeacon?.let { beacon ->
                // Setze Typ
                val typeArray = resources.getStringArray(R.array.beacon_types)
                val typePosition = typeArray.indexOf(beacon.type)
                if (typePosition >= 0) {
                    typeSpinner.setSelection(typePosition)
                }
                
                // Setze Rate
                rateInput.setText(beacon.rate)
            }
        }
        
        // Zeige Kalibrierungsstatus
        val calibratedRssi = sharedPreferences.getInt("calibrated_rssi", -59)
        calibrationStatusText.text = "Kalibrierter RSSI-Wert: $calibratedRssi dBm"
    }
    
    private fun saveBeaconSettings() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("saved_beacons", null)
        
        if (json != null) {
            val type = object : TypeToken<List<BeaconData>>() {}.type
            val beacons = gson.fromJson<MutableList<BeaconData>>(json, type)
            
            val index = beacons.indexOfFirst { it.address == beaconAddress }
            if (index >= 0) {
                val updatedBeacon = beacons[index].copy(
                    type = typeSpinner.selectedItem.toString(),
                    rate = rateInput.text.toString()
                )
                beacons[index] = updatedBeacon
                
                // Speichere aktualisierte Liste
                sharedPreferences.edit()
                    .putString("saved_beacons", gson.toJson(beacons))
                    .apply()
                
                Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
} 