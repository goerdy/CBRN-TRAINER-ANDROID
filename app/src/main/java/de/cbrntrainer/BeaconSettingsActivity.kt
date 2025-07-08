package de.cbrntrainer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BeaconSettingsActivity : BaseActivity() {
    
    private lateinit var beaconInfoText: TextView
    private lateinit var typeSpinner: Spinner
    private lateinit var rateInput: EditText
    private lateinit var rssiValueText: TextView
    private var beaconAddress: String? = null
    private var beaconName: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon_settings)
        
        beaconInfoText = findViewById(R.id.beaconInfoText)
        typeSpinner = findViewById(R.id.typeSpinner)
        rateInput = findViewById(R.id.rateInput)
        rssiValueText = findViewById(R.id.rssiValueText)
        
        // Beacon-Adresse aus Intent holen
        beaconAddress = intent.getStringExtra("BEACON_ADDRESS")
        if (beaconAddress == null) {
            Toast.makeText(this, "Keine Beacon-Adresse übergeben", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Beacon-Namen aus gespeicherten Beacons holen
        beaconName = getBeaconName(beaconAddress!!)
        
        // Beacon-Info anzeigen
        val displayName = if (beaconName.isNullOrEmpty()) "Unbekanntes Gerät" else beaconName
        beaconInfoText.text = "Beacon: $displayName\nAdresse: $beaconAddress"
        
        // Spinner mit Beacon-Typen füllen
        val beaconTypes = resources.getStringArray(R.array.beacon_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, beaconTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter
        
        // Gespeicherte Werte laden
        loadSavedValues()
        
        // Kalibrierungsbutton
        findViewById<Button>(R.id.calibrateButton).setOnClickListener {
            val intent = Intent(this, CalibrationActivity::class.java)
            intent.putExtra("BEACON_ADDRESS", beaconAddress)
            intent.putExtra("BEACON_NAME", beaconName)
            startActivity(intent)
        }
        
        // Speichern-Button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveBeaconSettings()
        }
    }
    
    private fun getBeaconName(address: String): String? {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("saved_beacons", null)
        
        if (json != null) {
            val type = object : TypeToken<List<BeaconData>>() {}.type
            val savedBeacons: List<BeaconData> = gson.fromJson(json, type)
            
            val beacon = savedBeacons.find { it.address == address }
            return beacon?.name
        }
        
        return null
    }
    
    override fun onResume() {
        super.onResume()
        // Lade die kalibrierten RSSI-Werte neu, wenn wir von der Kalibrierungsaktivität zurückkehren
        loadCalibratedRssi()
    }
    
    private fun loadSavedValues() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        
        // Lade den gespeicherten Typ
        val savedType = sharedPreferences.getString("${beaconAddress}_type", "Strahler")
        val beaconTypes = resources.getStringArray(R.array.beacon_types)
        val typeIndex = beaconTypes.indexOf(savedType)
        if (typeIndex >= 0) {
            typeSpinner.setSelection(typeIndex)
        }
        
        // Lade die gespeicherte Rate
        val savedRate = sharedPreferences.getString("${beaconAddress}_rate", "5.0")
        rateInput.setText(savedRate)
        
        // Lade den kalibrierten RSSI-Wert
        loadCalibratedRssi()
    }
    
    private fun loadCalibratedRssi() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val calibratedRssi = sharedPreferences.getInt("${beaconAddress}_calibrated_rssi", 0)
        
        if (calibratedRssi != 0) {
            rssiValueText.text = "$calibratedRssi dBm"
        } else {
            rssiValueText.text = "Nicht kalibriert"
        }
    }
    
    private fun saveBeaconSettings() {
        val type = typeSpinner.selectedItem.toString()
        val rate = rateInput.text.toString()
        
        if (rate.isEmpty()) {
            Toast.makeText(this, "Bitte geben Sie eine Messrate ein", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Speichere die Einstellungen
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("${beaconAddress}_type", type)
        editor.putString("${beaconAddress}_rate", rate)
        editor.apply()
        
        Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
        finish()
    }
} 