package de.cbrntrainer

import android.Manifest
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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat

class CalibrationActivity : BaseActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var countdownTextView: TextView
    private lateinit var rssiTextView: TextView
    private lateinit var instructionTextView: TextView
    private lateinit var startButton: Button
    
    private var isCalibrating = false
    private var rssiValues = mutableListOf<Int>()
    private var beaconAddress: String? = null
    
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
        
        beaconAddress = intent.getStringExtra("BEACON_ADDRESS")
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        startButton.setOnClickListener {
            startCalibration()
        }
        
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    private fun startCalibration() {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }
        
        isCalibrating = true
        rssiValues.clear()
        startButton.isEnabled = false
        
        // Starte den Bluetooth-Scan
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
        }
        
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        }
        
        // Berechne den Durchschnitt der RSSI-Werte
        val averageRssi = if (rssiValues.isNotEmpty()) {
            rssiValues.average().toInt()
        } else {
            -59 // Standardwert falls keine Werte gesammelt wurden
        }
        
        // Speichere den kalibrierten Wert
        getSharedPreferences("BeaconPrefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("calibrated_rssi", averageRssi)
            .apply()
        
        Toast.makeText(this, "Kalibrierung abgeschlossen! Referenz-RSSI: $averageRssi dBm", Toast.LENGTH_LONG).show()
        finish()
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