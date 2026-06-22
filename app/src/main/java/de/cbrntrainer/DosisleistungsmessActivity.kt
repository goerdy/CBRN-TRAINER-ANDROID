package de.cbrntrainer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DosisleistungsmessActivity : BaseActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var meterWebView: WebView
    
    private val savedBeacons = mutableListOf<BeaconData>()
    private val beaconRates = mutableMapOf<String, Double>() // Aktuelle Raten pro Beacon
    private val rateWindow = mutableListOf<Double>()  // Speicher für die letzten Messwerte der Gesamtrate
    private val WINDOW_SIZE = 50  // Größe des Sliding Windows
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL = 800L  // 800ms = < 2 Updates pro Sekunde
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
        
        // Vollbildmodus für Messgeräte
        hideSystemUI()
        
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
        if (BluetoothPermissionHelper.hasPermissions(this)) {
            startScanning()
        } else {
            BluetoothPermissionHelper.requestPermissions(this, REQUEST_BLUETOOTH_PERMISSIONS)
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
        
        if (!BluetoothPermissionHelper.hasPermissions(this)) {
            BluetoothPermissionHelper.requestPermissions(this, REQUEST_BLUETOOTH_PERMISSIONS)
            return
        }
        
        // Überprüfen, ob Bluetooth aktiviert ist
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return
        }
        
        android.util.Log.d("DosisleistungsMess", "Starte Scan nach allen gespeicherten Beacons")
        
        // Scan-Einstellungen für kontinuierliches Scannen
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()
        
        // Starte den Scan
        bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
        isScanning = true
        
        // Initialisiere die Anzeige mit 0
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
        
        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            android.util.Log.e("DosisleistungsMess", "Fehler beim Stoppen des Scans: ${e.message}")
        }
        
        isScanning = false
    }
    
    private fun updateBeaconRate(beacon: BeaconData, rssi: Int) {
        // Berechne die Dosisleistung für dieses Beacon
        val rate = calculateDosisleistung(beacon, rssi)
        
        // Speichere die aktuelle Rate für dieses Beacon
        beaconRates[beacon.address] = rate
        
        // Berechne die Gesamtrate (Summe aller aktiven Beacons)
        val currentTime = System.currentTimeMillis()
        val totalRate = calculateTotalRate(currentTime)
        
        // Aktualisiere die Anzeige, aber nicht zu oft
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
            lastUpdateTime = currentTime
            updateDisplay(totalRate)
        }
    }
    
    private fun calculateDosisleistung(beacon: BeaconData, rssi: Int): Double {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        
        // Hole den kalibrierten RSSI-Wert für dieses Beacon (RSSI bei 1m Entfernung)
        val calibratedRssi = sharedPreferences.getInt("${beacon.address}_calibrated_rssi", -59)
        
        // Hole die gespeicherte Rate für dieses Beacon (Strahlungswert in 10cm Entfernung in µSv/h)
        val baseRate = sharedPreferences.getString("${beacon.address}_rate", "5.0")?.toDoubleOrNull() ?: 5.0
        
        // Berechne die Entfernung in Metern basierend auf RSSI und kalibriertem Wert
        val n = 2.0 // Pfadverlustexponent für Freiraumausbreitung
        val distance = Math.pow(10.0, (calibratedRssi - rssi) / (10.0 * n))
        
        // Berechne die Dosisleistung basierend auf dem Abstandsgesetz
        // Wenn baseRate bei 10cm (0.1m) gemessen wurde, dann müssen wir die Formel anpassen:
        // Dosisleistung bei Abstand d = baseRate * (0.1/d)²
        
        // Umrechnung: Wenn baseRate bei 10cm gilt, wie ist die Rate bei 1m?
        val rateAt1m = baseRate * Math.pow(0.1/1.0, 2.0) // = baseRate * 0.01
        
        // Jetzt können wir die Dosisleistung am aktuellen Abstand berechnen
        val dosisleistung = rateAt1m * (1.0 / (distance * distance))
        
        android.util.Log.d("DosisleistungsMess", 
            "Beacon: ${beacon.name}, RSSI: $rssi, Kalibrierter RSSI: $calibratedRssi, " +
            "Abstand: ${String.format("%.2f", distance)}m, " +
            "Basisrate: $baseRate µSv/h, " +
            "Dosisleistung: ${String.format("%.4f", dosisleistung)} µSv/h")
        
        return dosisleistung
    }
    
    private fun calculateTotalRate(currentTime: Long): Double {
        // Entferne Beacons, die zu lange nicht gesehen wurden
        val beaconsToRemove = mutableListOf<String>()
        for ((address, lastSeen) in beaconLastSeen) {
            if (currentTime - lastSeen > SIGNAL_TIMEOUT) {
                beaconsToRemove.add(address)
                beaconRates.remove(address)
            }
        }
        
        beaconsToRemove.forEach { beaconLastSeen.remove(it) }
        
        // Berechne die Gesamtrate (Summe aller aktiven Beacons)
        val totalRate = beaconRates.values.sum()
        
        // Füge den neuen Wert zum Fenster hinzu
        rateWindow.add(totalRate)
        
        // Begrenze die Größe des Fensters
        while (rateWindow.size > WINDOW_SIZE) {
            rateWindow.removeAt(0)
        }
        
        // Berechne den geglätteten Wert (Durchschnitt des Fensters)
        return if (rateWindow.isNotEmpty()) rateWindow.average() else 0.0
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
                    val totalRate = calculateTotalRate(currentTime)
                    updateDisplay(totalRate)
                }
                
                // Plane die nächste Überprüfung
                if (isScanning) {
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
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
                if (BluetoothPermissionHelper.hasPermissions(this)) {
                    startScanning()
                } else {
                    BluetoothPermissionHelper.requestPermissions(this, REQUEST_BLUETOOTH_PERMISSIONS)
                }
            } else {
                Toast.makeText(this, "Bluetooth muss aktiviert sein, um die Dosisleistung zu messen", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Lade die gespeicherten Beacons
        loadSavedBeacons()
        
        if (BluetoothPermissionHelper.hasPermissions(this) && !isScanning) {
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
    
    // Füge diese Methode hinzu, um die Anzeige zu aktualisieren
    private fun updateDisplay(rate: Double) {
        runOnUiThread {
            val (value, unit) = when {
                rate >= 1000 -> {
                    String.format(java.util.Locale.US, "%.3f", rate / 1000) to "Sv/h"
                }
                rate >= 1.0 -> {
                    String.format(java.util.Locale.US, "%.3f", rate) to "mSv/h"
                }
                else -> {
                    String.format(java.util.Locale.US, "%.1f", rate * 1000) to "µSv/h"
                }
            }
            
            meterWebView.evaluateJavascript(
                "javascript:updateValue('$value', '$unit')",
                null
            )
        }
    }
    
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
        private const val REQUEST_ENABLE_BT = 2
    }
} 