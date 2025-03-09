package de.cbrntrainer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BluetoothSettingsActivity : BaseActivity() {
    
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListRecyclerView: RecyclerView
    private lateinit var savedDevicesRecyclerView: RecyclerView
    private lateinit var sharedPreferences: SharedPreferences
    
    private val discoveredBeacons = mutableListOf<BeaconData>()
    private val savedBeacons = mutableListOf<BeaconData>()
    private var isScanning = false
    
    private lateinit var beaconListAdapter: BeaconListAdapter
    private lateinit var savedBeaconAdapter: SavedBeaconAdapter
    
    private val scanHandler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 5000 // 5 Sekunden
    private val MAX_DEVICES = 20 // Maximale Anzahl von Geräten
    
    private lateinit var logTextView: TextView
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!isScanning) return  // Ignoriere Ergebnisse wenn Scan gestoppt
            
            // Stoppe Scan wenn zu viele Geräte gefunden wurden
            if (discoveredBeacons.size >= MAX_DEVICES) {
                stopBleScan()
                runOnUiThread {
                    Toast.makeText(applicationContext, 
                        "Scan gestoppt: Maximale Anzahl von Geräten erreicht", 
                        Toast.LENGTH_SHORT).show()
                }
                return
            }
            
            // Debug-Ausgabe
            logDebug("Gerät gefunden: ${result.device.address}, Name: ${result.device.name}, RSSI: ${result.rssi}")
            
            val device = result.device
            val beacon = BeaconData(
                address = device.address,
                name = device.name,
                rssi = result.rssi
            )
            
            runOnUiThread {
                // Prüfen, ob das Gerät bereits in der Liste ist
                val existingIndex = discoveredBeacons.indexOfFirst { it.address == beacon.address }
                if (existingIndex >= 0) {
                    // Aktualisiere das vorhandene Gerät
                    discoveredBeacons[existingIndex] = beacon
                    beaconListAdapter.notifyItemChanged(existingIndex)
                } else {
                    // Füge das neue Gerät hinzu
                    discoveredBeacons.add(beacon)
                    beaconListAdapter.notifyItemInserted(discoveredBeacons.size - 1)
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            logDebug("Scan fehlgeschlagen mit Fehlercode: $errorCode")
            runOnUiThread {
                Toast.makeText(applicationContext, "Bluetooth-Scan fehlgeschlagen (Fehler: $errorCode)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_settings)
        
        // TextView für Logs initialisieren
        logTextView = findViewById(R.id.logTextView)
        logDebug("BluetoothSettingsActivity gestartet")
        
        // Bluetooth-Adapter initialisieren
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        // Prüfen, ob Bluetooth verfügbar ist
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth wird auf diesem Gerät nicht unterstützt", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        // SharedPreferences für gespeicherte Beacons
        sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        
        // RecyclerViews initialisieren
        deviceListRecyclerView = findViewById(R.id.deviceList)
        savedDevicesRecyclerView = findViewById(R.id.savedDevicesList)
        
        // Stellen Sie sicher, dass die RecyclerViews einen festen Größe haben
        deviceListRecyclerView.setHasFixedSize(true)
        savedDevicesRecyclerView.setHasFixedSize(true)
        
        deviceListRecyclerView.layoutManager = LinearLayoutManager(this)
        savedDevicesRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Adapter initialisieren
        beaconListAdapter = BeaconListAdapter(discoveredBeacons) { beacon ->
            addBeaconToSaved(beacon)
        }
        
        savedBeaconAdapter = SavedBeaconAdapter(
            savedBeacons,
            onRemoveClick = { beacon -> removeBeaconFromSaved(beacon) },
            onSettingsClick = { beacon ->
                // Starte Einstellungen für dieses Beacon
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
        
        deviceListRecyclerView.adapter = beaconListAdapter
        savedDevicesRecyclerView.adapter = savedBeaconAdapter
        
        // Überprüfen, ob die Adapter korrekt gesetzt wurden
        logDebug("Adapter initialisiert: deviceList=${deviceListRecyclerView.adapter != null}, savedList=${savedDevicesRecyclerView.adapter != null}")
        
        // Gespeicherte Beacons laden
        loadSavedBeacons()
        
        // Scan-Button
        findViewById<Button>(R.id.scanButton).setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                // Bluetooth aktivieren
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val enableBtIntent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    Toast.makeText(this, "Bluetooth-Berechtigungen werden benötigt", Toast.LENGTH_LONG).show()
                    requestBluetoothPermissions()
                }
                return@setOnClickListener
            }
            
            if (checkBluetoothPermissions()) {
                startBleScan()
            } else {
                requestBluetoothPermissions()
            }
        }
        
        // Zurück-Button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth wurde aktiviert, starte den Scan
                if (checkBluetoothPermissions()) {
                    startBleScan()
                } else {
                    requestBluetoothPermissions()
                }
            } else {
                Toast.makeText(this, "Bluetooth muss aktiviert sein, um Beacons zu scannen", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startBleScan() {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }
        
        // Liste leeren beim Start eines neuen Scans
        discoveredBeacons.clear()
        beaconListAdapter.notifyDataSetChanged()
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            isScanning = true
            bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
            logDebug("Bluetooth-Scan gestartet")
            
            // Stoppe den Scan nach SCAN_PERIOD
            scanHandler.postDelayed({
                stopBleScan()
            }, SCAN_PERIOD)
        }
    }
    
    private fun stopBleScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            isScanning = false
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
            logDebug("Bluetooth-Scan gestoppt")
        }
    }
    
    private fun addBeaconToSaved(beacon: BeaconData) {
        // Prüfen, ob das Beacon bereits gespeichert ist
        if (savedBeacons.none { it.address == beacon.address }) {
            val savedBeacon = beacon.copy(isSaved = true)
            savedBeacons.add(savedBeacon)
            savedBeaconAdapter.notifyItemInserted(savedBeacons.size - 1)
            saveBeacons()
            Toast.makeText(this, "Beacon hinzugefügt", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Beacon bereits gespeichert", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeBeaconFromSaved(beacon: BeaconData) {
        val index = savedBeacons.indexOfFirst { it.address == beacon.address }
        if (index >= 0) {
            savedBeacons.removeAt(index)
            savedBeaconAdapter.notifyDataSetChanged()
            saveBeacons()
            Toast.makeText(this, "Beacon entfernt", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveBeacons() {
        val gson = Gson()
        val json = gson.toJson(savedBeacons)
        sharedPreferences.edit().putString("saved_beacons", json).apply()
    }
    
    private fun loadSavedBeacons() {
        val gson = Gson()
        val json = sharedPreferences.getString("saved_beacons", null)
        if (json != null) {
            val type = object : TypeToken<List<BeaconData>>() {}.type
            val loadedBeacons: List<BeaconData> = gson.fromJson(json, type)
            savedBeacons.clear()
            savedBeacons.addAll(loadedBeacons)
            savedBeaconAdapter.notifyDataSetChanged()
        }
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_BLUETOOTH_PERMISSIONS
        )
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBleScan()
            } else {
                Toast.makeText(this, "Bluetooth-Berechtigungen werden benötigt", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopBleScan()
    }
    
    private fun addLog(message: String) {
        runOnUiThread {
            val currentText = logTextView.text.toString()
            val newText = "$message\n$currentText"
            logTextView.text = newText
        }
    }
    
    private fun logDebug(message: String) {
        android.util.Log.d("BluetoothScan", message)
        addLog(message)
    }
    
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
        private const val REQUEST_ENABLE_BT = 2
    }
} 