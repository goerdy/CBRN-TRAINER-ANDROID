package de.cbrntrainer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class CalibrationActivity : BaseActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var countdownTextView: TextView
    private lateinit var rssiTextView: TextView
    private lateinit var instructionTextView: TextView
    private lateinit var startButton: Button
    private lateinit var titleTextView: TextView
    
    private var isCalibrating = false
    private var rssiValues = mutableListOf<Int>()
    private var beaconAddress: String? = null
    private var beaconName: String? = null
    
    private val handler = Handler(Looper.getMainLooper())
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.address == beaconAddress && isCalibrating) {
                rssiValues.add(result.rssi)
                rssiTextView.text = "Aktueller RSSI: ${result.rssi} dBm"
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)
        
        countdownTextView = findViewById(R.id.countdownTextView)
        rssiTextView = findViewById(R.id.rssiTextView)
        instructionTextView = findViewById(R.id.instructionTextView)
        startButton = findViewById(R.id.startButton)
        titleTextView = findViewById(R.id.calibrationTitle)
        
        // Blende den Zurück-Button aus
        findViewById<Button>(R.id.backButton).visibility = View.GONE
        
        beaconAddress = intent.getStringExtra("BEACON_ADDRESS")
        beaconName = intent.getStringExtra("BEACON_NAME")
        
        // Setze den Titel mit dem Beacon-Namen
        val displayName = if (beaconName.isNullOrEmpty()) "Unbekanntes Gerät" else beaconName
        titleTextView.text = "Kalibrierung: $displayName"
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        startButton.setOnClickListener {
            startCalibration()
        }
    }
    
    private fun startCalibration() {
        if (!BluetoothPermissionHelper.hasPermissions(this)) {
            BluetoothPermissionHelper.requestPermissions(this, REQUEST_BLUETOOTH_PERMISSIONS)
            return
        }
        
        isCalibrating = true
        rssiValues.clear()
        startButton.isEnabled = false
        
        // Starte den Bluetooth-Scan
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
        
        // Starte den Countdown
        object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                countdownTextView.text = "Noch $seconds Sekunden"
            }
            
            override fun onFinish() {
                finishCalibration()
            }
        }.start()
        
        instructionTextView.text = "Bitte halten Sie das Gerät einen Meter vom Beacon entfernt..."
    }
    
    private fun finishCalibration() {
        isCalibrating = false
        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            android.util.Log.e("Calibration", "Fehler beim Stoppen des Scans: ${e.message}")
        }
        
        // Berechne den Durchschnitt der RSSI-Werte
        val averageRssi = if (rssiValues.isNotEmpty()) {
            rssiValues.average().toInt()
        } else {
            -59 // Standardwert falls keine Werte gesammelt wurden
        }
        
        // Speichere den kalibrierten Wert für diesen spezifischen Beacon
        beaconAddress?.let { address ->
            getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("${address}_calibrated_rssi", averageRssi)
                .apply()
            
            val displayName = if (beaconName.isNullOrEmpty()) "Beacon" else beaconName
            Toast.makeText(this, "Kalibrierung von $displayName abgeschlossen! Referenz-RSSI: $averageRssi dBm", Toast.LENGTH_LONG).show()
        } ?: Toast.makeText(this, "Fehler: Keine Beacon-Adresse", Toast.LENGTH_SHORT).show()
        
        finish()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCalibration()
            } else {
                Toast.makeText(this, "Bluetooth-Berechtigungen werden benötigt", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1
    }
} 