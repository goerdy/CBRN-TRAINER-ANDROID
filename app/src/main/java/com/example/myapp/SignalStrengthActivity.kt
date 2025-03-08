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
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SignalStrengthActivity : BaseActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var meterWebView: WebView
    
    private var beaconAddress: String? = null
    private var beaconName: String? = null
    private var baseRate: Double = 0.0  // Gespeicherte Rate in 10cm
    private val rateWindow = mutableListOf<Double>()  // Speicher für die letzten Messwerte
    private val WINDOW_SIZE = 100  // Größe des Sliding Windows
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL = 200L  // 200ms = 5 Updates pro Sekunde
    private var lastValidScanTime = 0L
    private val SIGNAL_TIMEOUT = 2000L  // 2 Sekunden bis Signal als verloren gilt
    
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address == beaconAddress) {
                android.util.Log.d("SignalStrength", "Gesuchtes Beacon gefunden! RSSI: ${result.rssi}")
                updateSignalStrength(result.rssi)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("SignalStrength", "Scan fehlgeschlagen mit Fehlercode: $errorCode")
            runOnUiThread {
                Toast.makeText(applicationContext, "Bluetooth-Scan fehlgeschlagen (Fehler: $errorCode)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private var lastSuccessfulScanTime: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signal_strength)
        
        // Initialisiere WebView
        meterWebView = findViewById(R.id.meterWebView)
        meterWebView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true  // Aktiviere DOM Storage
        }
        meterWebView.webChromeClient = android.webkit.WebChromeClient()  // Für JavaScript Konsole
        
        // Debug-Callback für JavaScript Fehler
        meterWebView.setWebViewClient(object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                android.util.Log.d("WebView", "Seite geladen")
                // Test-Update nach dem Laden
                meterWebView.evaluateJavascript(
                    "javascript:updateValue('0.0', 'µSv/h')",
                    { result -> android.util.Log.d("WebView", "JavaScript Result: $result") }
                )
            }
        })
        meterWebView.loadUrl("file:///android_asset/meter.html")
        
        // Hole Beacon-Informationen aus dem Intent
        beaconAddress = intent.getStringExtra("BEACON_ADDRESS")
        beaconName = intent.getStringExtra("BEACON_NAME") ?: "Unbekanntes Gerät"
        
        // Lade die gespeicherte Rate für dieses Beacon
        loadBeaconRate()
        
        // Debug-Ausgabe für die Beacon-Adresse
        android.util.Log.d("SignalStrength", "Suche nach Beacon: $beaconName mit Adresse: $beaconAddress")
        
        // Überprüfe, ob die Adresse im korrekten Format ist (z.B. "00:11:22:33:44:55")
        if (beaconAddress == null || beaconAddress?.matches(Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")) != true) {
            android.util.Log.e("SignalStrength", "Ungültiges Adressformat: $beaconAddress")
            Toast.makeText(this, "Ungültiges Adressformat: $beaconAddress", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
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
        
        android.util.Log.d("SignalStrength", "Starte Scan für Beacon: $beaconAddress")
        Toast.makeText(this, "Suche nach Beacon...", Toast.LENGTH_SHORT).show()
        
        // Erstelle einen Filter für das spezifische Beacon
        // Wir scannen nach allen Geräten, um sicherzustellen, dass wir das Beacon finden
        // und filtern dann in onScanResult
        
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
        
        // Aktualisiere die UI, wenn kein Signal empfangen wird
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isScanning) {
                    // Überprüfe, ob wir noch scannen und aktualisiere die UI
                    android.util.Log.d("SignalStrength", "Scan läuft noch...")
                    // Wenn wir noch scannen, plane die nächste Aktualisierung
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
        
        startSignalLossChecker()
    }
    
    private fun stopScanning() {
        if (!isScanning) return
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        }
        
        isScanning = false
    }
    
    private fun formatDoseRate(rateInMsv: Double): String {
        return when {
            rateInMsv >= 1000 -> String.format("%.3f Sv/h", rateInMsv / 1000)
            rateInMsv >= 1 -> String.format("%.3f mSv/h", rateInMsv)
            else -> String.format("%.1f µSv/h", rateInMsv * 1000)
        }
    }
    
    private fun updateSignalStrength(rssi: Int) {
        val currentTime = System.currentTimeMillis()
        lastValidScanTime = currentTime
        
        if (baseRate == 0.0) {
            android.util.Log.e("SignalStrength", "Base Rate ist 0, Messung nicht möglich!")
            return
        }
        
        // Berechne die Entfernung mit dem kalibrierten RSSI-Wert
        val calibratedRssi = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
            .getInt("calibrated_rssi", -59)
        
        android.util.Log.d("SignalStrength", "Kalibrierter RSSI: $calibratedRssi, Aktueller RSSI: $rssi")
        
        val pathLossExponent = 2.0
        val distance = Math.pow(10.0, (calibratedRssi - rssi) / (10.0 * pathLossExponent))
        android.util.Log.d("SignalStrength", "Berechnete Entfernung: $distance m")
        
        // Berechne die aktuelle Dosisleistung nach dem Abstandsquadratgesetz
        val currentRate = baseRate * Math.pow(0.1 / distance, 2.0)
        android.util.Log.d("SignalStrength", "Base Rate: $baseRate, Berechnete Rate: $currentRate")
        
        // Begrenze auf 200% der eingestellten Aktivität
        val limitedRate = currentRate.coerceIn(0.0, baseRate * 2.0)
        
        // Füge den neuen Wert zum Window hinzu
        rateWindow.add(limitedRate)
        
        // Entferne den ältesten Wert, wenn das Window voll ist
        if (rateWindow.size > WINDOW_SIZE) {
            rateWindow.removeAt(0)
        }
        
        // Berechne den Durchschnitt der letzten Werte
        val averageRate = if (rateWindow.isNotEmpty()) {
            rateWindow.average()
        } else {
            limitedRate
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
                android.util.Log.d("SignalStrength", "Rate: $averageRate, Display: $value $unit")
                
                val js = "javascript:updateValue('$value', '$unit')"
                android.util.Log.d("WebView", "Sende JS: $js")
                meterWebView.evaluateJavascript(
                    js,
                    { result -> android.util.Log.d("WebView", "JavaScript Result: $result") }
                )
            }
            
            lastUpdateTime = currentTime
        }
        
        // Speichere den letzten erfolgreichen Scan-Zeitpunkt
        lastSuccessfulScanTime = currentTime
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
                Toast.makeText(this, "Bluetooth muss aktiviert sein, um die Signalstärke zu messen", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun loadBeaconRate() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("saved_beacons", null)
        
        android.util.Log.d("SignalStrength", "Lade gespeicherte Beacons: $json")
        
        if (json != null) {
            val type = object : TypeToken<List<BeaconData>>() {}.type
            val beacons: List<BeaconData> = gson.fromJson(json, type)
            val beacon = beacons.find { it.address == beaconAddress }
            
            android.util.Log.d("SignalStrength", "Gefundenes Beacon: $beacon")
            baseRate = beacon?.rate?.toDoubleOrNull() ?: run {
                android.util.Log.e("SignalStrength", "Keine gültige Rate gefunden für Beacon: $beaconAddress")
                0.0
            }
            android.util.Log.d("SignalStrength", "Geladene Base Rate: $baseRate")
            
            if (baseRate == 0.0) {
                Toast.makeText(this, "Keine kalibrierte Rate gefunden!", Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.e("SignalStrength", "Keine gespeicherten Beacons gefunden!")
            Toast.makeText(this, "Keine gespeicherten Beacons gefunden!", Toast.LENGTH_LONG).show()
        }
    }
    
    // Überprüfe regelmäßig, ob das Signal verloren wurde
    private fun startSignalLossChecker() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastValidScanTime > SIGNAL_TIMEOUT) {
                    runOnUiThread {
                        val (value, unit) = when {
                            false -> {
                                String.format("%.3f", 0.0) to "mSv/h"
                            }
                            else -> {
                                String.format("%.1f", 0.0) to "µSv/h"
                            }
                        }
                        meterWebView.evaluateJavascript(
                            "javascript:updateValue('$value', '$unit')",
                            null
                        )
                    }
                }
                if (isScanning) {
                    handler.postDelayed(this, UPDATE_INTERVAL)
                }
            }
        }, UPDATE_INTERVAL)
    }
    
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
        private const val REQUEST_ENABLE_BT = 2
    }
} 