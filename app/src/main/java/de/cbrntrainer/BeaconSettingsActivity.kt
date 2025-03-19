package de.cbrntrainer

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class BeaconSettingsActivity : BaseActivity() {
    
    private lateinit var beaconAddress: String
    private lateinit var beaconName: String
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon_settings)
        
        // Beacon-Daten aus Intent holen
        beaconAddress = intent.getStringExtra("BEACON_ADDRESS") ?: ""
        beaconName = intent.getStringExtra("BEACON_NAME") ?: "Unbekanntes Gerät"
        
        if (beaconAddress.isEmpty()) {
            Toast.makeText(this, "Fehler: Keine Beacon-Adresse übergeben", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Beacon-Info anzeigen
        val beaconInfoText = findViewById<TextView>(R.id.beaconInfoText)
        beaconInfoText.text = "Beacon: $beaconName\nAdresse: $beaconAddress"
        
        // Gerätetyp-Spinner einrichten
        setupTypeSpinner()
        
        // Messrate-Eingabefeld einrichten
        setupRateInput()
        
        // Speichern-Button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            saveBeaconSettings()
        }
    }
    
    private fun setupTypeSpinner() {
        val spinner = findViewById<Spinner>(R.id.typeSpinner)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.beacon_types,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        // Gespeicherten Typ laden
        val sharedPreferences = getSharedPreferences("BeaconSettings", Context.MODE_PRIVATE)
        val savedType = sharedPreferences.getString("${beaconAddress}_type", "Strahler")
        val position = adapter.getPosition(savedType)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }
    
    private fun setupRateInput() {
        val rateInput = findViewById<EditText>(R.id.rateInput)
        
        // Gespeicherte Rate laden
        val sharedPreferences = getSharedPreferences("BeaconSettings", Context.MODE_PRIVATE)
        val savedRate = sharedPreferences.getString("${beaconAddress}_rate", "5.0")
        rateInput.setText(savedRate)
    }
    
    private fun saveBeaconSettings() {
        val typeSpinner = findViewById<Spinner>(R.id.typeSpinner)
        val rateInput = findViewById<EditText>(R.id.rateInput)
        
        val type = typeSpinner.selectedItem.toString()
        val rate = rateInput.text.toString()
        
        // Einstellungen speichern
        val sharedPreferences = getSharedPreferences("BeaconSettings", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("${beaconAddress}_type", type)
            .putString("${beaconAddress}_rate", rate)
            .apply()
        
        // Beacon als gespeichert markieren
        val savedBeacons = sharedPreferences.getStringSet("saved_beacons", mutableSetOf()) ?: mutableSetOf()
        val updatedBeacons = savedBeacons.toMutableSet()
        updatedBeacons.add(beaconAddress)
        sharedPreferences.edit()
            .putStringSet("saved_beacons", updatedBeacons)
            .apply()
        
        // Beacon-Name speichern
        sharedPreferences.edit()
            .putString("${beaconAddress}_name", beaconName)
            .apply()
        
        Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
        finish()
    }
} 