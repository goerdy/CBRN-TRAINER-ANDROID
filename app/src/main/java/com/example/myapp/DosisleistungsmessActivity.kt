package com.example.myapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DosisleistungsmessActivity : BaseActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var meterWebView: WebView
    
    private val savedBeacons = mutableListOf<BeaconData>()
    private val beaconRates = mutableMapOf<String, Double>() // Aktuelle Raten pro Beacon
    private val rateWindow = mutableListOf<Double>()  // Speicher für die letzten Messwerte der Gesamtrate
    private val WINDOW_SIZE = 100  // Größe des Sliding Windows
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL = 200L  // 200ms = 5 Updates pro Sekunde
    private val SIGNAL_TIMEOUT = 5000L  // 5 Sekunden bis Signal als verloren gilt
    private val beaconLastSeen = mutableMapOf<String, Long>() // Wann wurde ein Beacon zuletzt gesehen
    
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Prüfe, ob das gefundene Gerät in unserer Liste gespeicherter Beacons ist
            val beacon = savedBeacons.find { it.address == result.device.address }
            if (beacon != null) {
                android.util.Log.d("DosisleistungsMess", "Gespeichertes Beacon gefunden: ${beacon.name} (${beacon.address}) RSSI: ${result.rssi}")
                updateBeaconRate(beacon, result.rssi)
                beaconLastSeen[beacon.address] = System.currentTimeMillis()
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("DosisleistungsMess", "Scan fehlgeschlagen mit Fehlercode: $errorCode")
            runOnUiThread {
                Toast.makeText(applicationContext, "Bluetooth-Scan fehlgeschlagen (Fehler: $errorCode)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dosisleistungsmess)
        
        // Initialisiere WebView
        meterWebView = findViewById(R.id.meterWebView)
        meterWebView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
        }
        meterWebView.webChromeClient = android.webkit.WebChromeClient()
        
        meterWebView.setWebViewClient(object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.d("WebView", "Seite geladen")
                meterWebView.evaluateJavascript(
                    "javascript:updateValue('0.0', 'µSv/h')",
                    { result -> android.util.Log.d("WebView", "JavaScript Result: $result") }
                )
            }
        })
        meterWebView.loadUrl("file:///android_asset/meter.html")
        
        // Lade gespeicherte Beacons
        loadSavedBeacons()
        
        // Initialisiere Bluetooth
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        // Prüfe, ob Bluetooth verfügbar ist
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth wird auf diesem Gerät nicht unterstützt", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        // Starte den Scan, wenn die Berechtigungen vorhanden sind
        if (checkBluetoothPermissions()) {
            startScanning()
            // Starte einen Timer, um den Scan regelmäßig neu zu starten
            startScanRefreshTimer()
        } else {
            requestBluetoothPermissions()
        }
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
            
            // Initialisiere alle Beacons mit Rate 0
            for (beacon in savedBeacons) {
                beaconRates[beacon.address] = 0.0
                beaconLastSeen[beacon.address] = 0L
            }
            
            android.util.Log.d("DosisleistungsMess", "Geladene Beacons: ${savedBeacons.size}")
            
            if (savedBeacons.isEmpty()) {
                Toast.makeText(this, "Keine gespeicherten Beacons gefunden!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Keine gespeicherten Beacons gefunden!", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun startScanning() {
        if (isScanning) return
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissions()
            return
        }
        
        // Überprüfen, ob Bluetooth aktiviert ist
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                Toast.makeText(this, "Bluetooth-Berechtigungen werden benötigt", Toast.LENGTH_LONG).show()
                requestBluetoothPermissions()
            }
            return
        }
        
        android.util.Log.d("DosisleistungsMess", "Starte Scan nach allen gespeicherten Beacons")
        Toast.makeText(this, "Suche nach Beacons...", Toast.LENGTH_SHORT).show()
        
        // Scan-Einstellungen für kontinuierliches Scannen
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()
        
        // Starte den Scan
        bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
        isScanning = true
        
        runOnUiThread {
            meterWebView.evaluateJavascript(
                "javascript:updateValue('0.0', 'µSv/h')",
                null
            )
        }
        
        // Starte regelmäßige Überprüfung der Beacon-Timeouts
        startBeaconTimeoutChecker()
    }
    
    private fun stopScanning() {
        if (!isScanning) return
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        }
        
        isScanning = false
    }
    
    private fun updateBeaconRate(beacon: BeaconData, rssi: Int) {
        val baseRate = beacon.rate.toDoubleOrNull() ?: 0.0
        if (baseRate == 0.0) {
            android.util.Log.d("DosisleistungsMess", "Beacon ${beacon.name} hat Rate 0, wird ignoriert")
            return
        }
        
        // Berechne die Entfernung mit dem kalibrierten RSSI-Wert
        val calibratedRssi = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
            .getInt("calibrated_rssi", -59)
        
        val pathLossExponent = 2.0
        val distance = Math.pow(10.0, (calibratedRssi - rssi) / (10.0 * pathLossExponent))
        
        // Berechne die aktuelle Dosisleistung nach dem Abstandsquadratgesetz
        val currentRate = baseRate * Math.pow(0.1 / distance, 2.0)
        
        // Begrenze auf 200% der eingestellten Aktivität
        val limitedRate = currentRate.coerceIn(0.0, baseRate * 2.0)
        
        // Aktualisiere die Rate für dieses Beacon
        beaconRates[beacon.address] = limitedRate
        
        // Berechne die Gesamtrate aller Beacons
        updateTotalRate()
    }
    
    private fun updateTotalRate() {
        val currentTime = System.currentTimeMillis()
        
        // Berechne die Summe aller aktuellen Beacon-Raten
        val totalRate = beaconRates.values.sum()
        
        // Füge den neuen Wert zum Window hinzu
        rateWindow.add(totalRate)
        
        // Entferne den ältesten Wert, wenn das Window voll ist
        if (rateWindow.size > WINDOW_SIZE) {
            rateWindow.removeAt(0)
        }
        
        // Berechne den Durchschnitt der letzten Werte
        val averageRate = if (rateWindow.isNotEmpty()) {
            rateWindow.average()
        } else {
            totalRate
        }
        
        // Aktualisiere die UI nur alle UPDATE_INTERVAL Millisekunden
        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
            runOnUiThread {
                val (value, unit) = when {
                    averageRate >= 1.0 -> {
                        String.format(java.util.Locale.US, "%.3f", averageRate) to "mSv/h"
                    }
                    else -> {
                        String.format(java.util.Locale.US, "%.1f", averageRate * 1000) to "µSv/h"
                    }
                }
                
                meterWebView.evaluateJavascript(
                    "javascript:updateValue('$value', '$unit')",
                    null
                )
            }
            
            lastUpdateTime = currentTime
        }
    }
    
    private fun startBeaconTimeoutChecker() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                var ratesUpdated = false
                
                // Prüfe für jedes Beacon, ob es noch aktuell ist
                for (address in beaconLastSeen.keys) {
                    val lastSeen = beaconLastSeen[address] ?: 0L
                    if (currentTime - lastSeen > SIGNAL_TIMEOUT && beaconRates[address] != 0.0) {
                        // Beacon ist nicht mehr in Reichweite, setze Rate auf 0
                        beaconRates[address] = 0.0
                        ratesUpdated = true
                        android.util.Log.d("DosisleistungsMess", "Beacon $address nicht mehr in Reichweite, Rate auf 0 gesetzt")
                    }
                }
                
                // Wenn sich Raten geändert haben, aktualisiere die Gesamtrate
                if (ratesUpdated) {
                    updateTotalRate()
                }
                
                // Plane die nächste Überprüfung
                if (isScanning) {
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }
    
    private fun startScanRefreshTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isScanning) {
                    // Stoppe den aktuellen Scan
                    stopScanning()
                    // Starte einen neuen Scan
                    startScanning()
                    // Plane die nächste Aktualisierung
                    handler.postDelayed(this, 10000) // Alle 10 Sekunden
                }
            }
        }, 10000) // Erste Aktualisierung nach 10 Sekunden
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
                startScanning()
            } else {
                Toast.makeText(this, "Bluetooth-Berechtigungen werden benötigt", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth wurde aktiviert, starte den Scan
                if (checkBluetoothPermissions()) {
                    startScanning()
                    startScanRefreshTimer()
                } else {
                    requestBluetoothPermissions()
                }
            } else {
                Toast.makeText(this, "Bluetooth muss aktiviert sein, um die Dosisleistung zu messen", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (checkBluetoothPermissions() && !isScanning) {
            startScanning()
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopScanning()
        rateWindow.clear()  // Leere das Window beim Pausieren
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
    }
    
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
        private const val REQUEST_ENABLE_BT = 2
    }
} 