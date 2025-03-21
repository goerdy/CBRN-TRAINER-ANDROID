package de.cbrntrainer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList

class BluetoothDLWarnActivity : BaseActivity() {
    private lateinit var webView: WebView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var sharedPreferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    
    private var isScanning = false
    private var savedBeacons = mutableListOf<BeaconData>()
    private var currentValue = 0.0
    
    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSIONS = 2
        private const val SCAN_PERIOD = 1000L
        private const val TAG = "BluetoothDLWarnActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_dl_warn)
        
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("file:///android_asset/dlwarner.html")
        
        sharedPreferences = getSharedPreferences("BeaconSettings", Context.MODE_PRIVATE)
        
        // Bluetooth initialisieren
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        // Prüfen, ob Bluetooth unterstützt wird
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth wird auf diesem Gerät nicht unterstützt", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Berechtigungen prüfen und anfordern
        checkAndRequestPermissions()
        
        // Gespeicherte Beacons laden
        loadSavedBeacons()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Bluetooth aktivieren, falls nicht aktiv
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                Toast.makeText(this, "Bluetooth-Berechtigung fehlt", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Starte Bluetooth-Scan
            startScan()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopScan()
        // Stoppe den Alarm, wenn die Activity pausiert wird
        webView.evaluateJavascript("javascript:stopAlarm()", null)
    }
    
    override fun onDestroy() {
        // Stoppe den Alarm, wenn die Activity zerstört wird
        webView.evaluateJavascript("javascript:stopAlarm()", null)
        super.onDestroy()
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_PERMISSIONS) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            
            if (allGranted) {
                startScan()
            } else {
                Toast.makeText(this, "Ohne die erforderlichen Berechtigungen kann die App nicht funktionieren", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun startScan() {
        if (isScanning) return
        
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth-Scan-Berechtigung fehlt", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
            
            // Regelmäßig den Scan neu starten
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (isScanning) {
                        bluetoothLeScanner.stopScan(scanCallback)
                        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
                        handler.postDelayed(this, SCAN_PERIOD)
                    }
                }
            }, SCAN_PERIOD)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten des Bluetooth-Scans", e)
            Toast.makeText(this, "Fehler beim Starten des Bluetooth-Scans: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopScan() {
        if (!isScanning) return
        
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        try {
            bluetoothLeScanner.stopScan(scanCallback)
            isScanning = false
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Stoppen des Bluetooth-Scans", e)
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            val device = result.device
            val address = device.address
            val rssi = result.rssi
            
            // Prüfen, ob das Beacon in der Liste der gespeicherten Beacons ist
            val savedBeacon = savedBeacons.find { it.address == address }
            
            if (savedBeacon != null) {
                // Berechne die Dosisleistung basierend auf der Signalstärke
                val dosisleistung = calculateDosisleistung(savedBeacon, rssi)
                
                // Aktualisiere den Wert nur, wenn er sich geändert hat
                if (dosisleistung != currentValue) {
                    currentValue = dosisleistung
                    updateUI(currentValue)
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Bluetooth-Scan fehlgeschlagen mit Fehlercode: $errorCode")
            Toast.makeText(applicationContext, "Bluetooth-Scan fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun calculateDosisleistung(beacon: BeaconData, rssi: Int): Double {
        // Hole die gespeicherte Rate für dieses Beacon
        val rate = sharedPreferences.getString("${beacon.address}_rate", "5.0")?.toDoubleOrNull() ?: 5.0
        
        // Berechne die Entfernung basierend auf RSSI
        // Einfache Formel: 10^((TxPower - RSSI)/(10 * n))
        // Wobei TxPower die Signalstärke in 1m Entfernung ist (ca. -59 dBm für viele BLE-Geräte)
        // und n der Pfadverlustexponent ist (ca. 2 für Freiraumausbreitung)
        val txPower = -59
        val n = 2.0
        val distance = Math.pow(10.0, (txPower - rssi) / (10.0 * n))
        
        // Berechne die Dosisleistung basierend auf dem Abstandsgesetz
        // Dosisleistung = Rate / (Entfernung^2)
        return rate / (distance * distance)
    }
    
    private fun loadSavedBeacons() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPreferences.getString("saved_beacons", null)
        
        if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<BeaconData>>() {}.type
            val loadedBeacons: List<BeaconData> = gson.fromJson(json, type)
            
            savedBeacons.clear()
            savedBeacons.addAll(loadedBeacons)
            
            if (savedBeacons.isEmpty()) {
                Toast.makeText(this, "Keine Beacons gespeichert. Bitte fügen Sie zuerst Beacons in den Bluetooth-Einstellungen hinzu.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Keine Beacons gespeichert. Bitte fügen Sie zuerst Beacons in den Bluetooth-Einstellungen hinzu.", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun updateUI(value: Double) {
        // Update WebView mit dem Wert
        webView.evaluateJavascript(
            "javascript:updateValue($value)",
            null
        )
    }
} 