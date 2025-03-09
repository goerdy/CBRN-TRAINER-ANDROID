package de.cbrntrainer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BluetoothModeActivity : BaseActivity() {
    
    private lateinit var savedBeaconsList: RecyclerView
    private lateinit var noBeaconsMessage: TextView
    private val savedBeacons = mutableListOf<BeaconData>()
    
    private fun removeBeaconFromSaved(beacon: BeaconData) {
        val index = savedBeacons.indexOfFirst { it.address == beacon.address }
        if (index >= 0) {
            savedBeacons.removeAt(index)
            savedBeaconsList.adapter?.notifyItemRemoved(index)
            
            // Speichere die aktualisierte Liste
            val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
            val gson = Gson()
            val json = gson.toJson(savedBeacons)
            sharedPreferences.edit().putString("saved_beacons", json).apply()
            
            // Zeige die "Keine Beacons" Nachricht wenn die Liste leer ist
            if (savedBeacons.isEmpty()) {
                noBeaconsMessage.visibility = View.VISIBLE
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_mode)
        
        // Initialisiere Views
        savedBeaconsList = findViewById(R.id.savedBeaconsList)
        noBeaconsMessage = findViewById(R.id.noBeaconsMessage)
        
        // RecyclerView einrichten
        savedBeaconsList.layoutManager = LinearLayoutManager(this)
        
        // Adapter für gespeicherte Beacons
        val savedBeaconAdapter = SavedBeaconAdapter(
            savedBeacons,
            onRemoveClick = { beacon ->
                removeBeaconFromSaved(beacon)
            },
            onSettingsClick = { beacon ->
                // Starte Kalibrierungsaktivität für dieses Beacon
                val intent = Intent(this, BeaconSettingsActivity::class.java)
                intent.putExtra("BEACON_ADDRESS", beacon.address)
                intent.putExtra("BEACON_NAME", beacon.name)
                startActivity(intent)
            },
            onItemClick = { beacon ->
                // Starte die SignalStrengthActivity mit dem ausgewählten Beacon
                val intent = Intent(this, SignalStrengthActivity::class.java)
                intent.putExtra("BEACON_ADDRESS", beacon.address)
                intent.putExtra("BEACON_NAME", beacon.name)
                startActivity(intent)
            }
        )
        
        savedBeaconsList.adapter = savedBeaconAdapter
        
        // Lade gespeicherte Beacons
        loadSavedBeacons()
        
        // Prüfe, ob ein bestimmtes Beacon angefordert wurde
        val requestedBeaconAddress = intent.getStringExtra("BEACON_ADDRESS")
        if (requestedBeaconAddress != null) {
            // Suche nach dem angeforderten Beacon in der Liste der gespeicherten Beacons
            val requestedBeacon = savedBeacons.find { it.address == requestedBeaconAddress }
            
            if (requestedBeacon != null) {
                // Beacon gefunden, führe Aktion aus
                // Zum Beispiel: Zeige Signalstärke an
                Toast.makeText(this, "Verbinde mit Beacon: ${requestedBeacon.name ?: requestedBeacon.address}", Toast.LENGTH_SHORT).show()
            } else {
                // Beacon nicht gefunden
                Toast.makeText(this, "Das angeforderte Beacon ist nicht gespeichert. Bitte fügen Sie es zuerst hinzu.", Toast.LENGTH_LONG).show()
            }
        }
        
        // Zeige Nachricht, wenn keine Beacons gespeichert sind
        if (savedBeacons.isEmpty()) {
            noBeaconsMessage.visibility = View.VISIBLE
        } else {
            noBeaconsMessage.visibility = View.GONE
        }
        
        // Bluetooth-Einstellungen Button
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            val intent = Intent(this, BluetoothSettingsActivity::class.java)
            startActivity(intent)
        }
        
        // QR-Code scannen Button
        findViewById<Button>(R.id.scanQrButton).setOnClickListener {
            val intent = Intent(this, QrScannerActivity::class.java)
            startActivity(intent)
        }
        
        // Zurück-Button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Lade die Beacons neu, wenn die Activity wieder sichtbar wird
        loadSavedBeacons()
    }
    
    private fun loadSavedBeacons() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("saved_beacons", null)
        
        if (json != null) {
            val type = object : TypeToken<List<BeaconData>>() {}.type
            val loadedBeacons: List<BeaconData> = gson.fromJson(json, type)
            
            savedBeacons.clear()
            savedBeacons.addAll(loadedBeacons)
            savedBeaconsList.adapter?.notifyDataSetChanged()
            
            // Aktualisiere die Sichtbarkeit der Nachricht
            if (savedBeacons.isEmpty()) {
                noBeaconsMessage.visibility = View.VISIBLE
            } else {
                noBeaconsMessage.visibility = View.GONE
            }
        } else {
            noBeaconsMessage.visibility = View.VISIBLE
        }
    }
} 