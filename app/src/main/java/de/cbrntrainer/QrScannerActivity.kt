package de.cbrntrainer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

class QrScannerActivity : BaseActivity() {
    private lateinit var captureManager: CaptureManager
    private lateinit var barcodeView: DecoratedBarcodeView
    private val TAG = "QrScannerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        barcodeView = findViewById(R.id.barcode_scanner)
        
        // Quadratisches Scan-Fenster konfigurieren
        val viewFinder = barcodeView.viewFinder
        viewFinder.setLaserVisibility(false) // Laser-Linie ausblenden
        viewFinder.setMaskColor(resources.getColor(R.color.scanner_mask, theme)) // Hintergrundfarbe
        
        // CaptureManager für die Kamera-Steuerung
        captureManager = CaptureManager(this, barcodeView)
        captureManager.initializeFromIntent(intent, savedInstanceState)
        
        // Callback für gescannte QR-Codes
        barcodeView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleScanResult(result.text)
            }
        })
        
        // Zurück-Button
        findViewById<android.widget.Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        captureManager.onSaveInstanceState(outState)
    }

    private fun handleScanResult(result: String) {
        Log.d(TAG, "Scanned QR code: $result")
        
        // Hole die Server-URL aus den SharedPreferences
        val sharedPreferences = getSharedPreferences(CloudSettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val serverUrl = sharedPreferences.getString(CloudSettingsActivity.URL_KEY, CloudSettingsActivity.DEFAULT_URL)

        // QR-Code-Inhalt verarbeiten
        when {
            // Fall 1: Web-Link mit TRAINEE.php
            serverUrl != null && result.startsWith("$serverUrl/TRAINEE.php") -> {
                // Extrahiere die Session-ID aus dem Link
                val sessionId = extractSessionId(result)
                Log.d(TAG, "Extracted session ID from URL: $sessionId")
                if (sessionId != null) {
                    // Speichere die Session-ID mit eigenem Key
                    sharedPreferences.edit()
                        .putString("last_session_id", sessionId)
                        .apply()

                    // Starte CloudModeLauncherActivity
                    val intent = Intent(this, CloudModeLauncherActivity::class.java)
                    intent.putExtra("SESSION_ID", sessionId)
                    startActivity(intent)
                    finish()
                } else {
                    showInvalidQrCodeMessage()
                }
            }
            
            // Fall 2: Deep Link (cbrn-trainer://)
            result.startsWith("cbrn-trainer://") -> {
                Log.d(TAG, "Processing deep link")
                // Deep Link verarbeiten
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse(result)
                startActivity(intent)
                finish()
            }
            
            // Fall 3: Bluetooth-Beacon-Format
            result.startsWith("cbrn-bt:") -> {
                Log.d(TAG, "Processing Bluetooth beacon")
                // Hier später die Bluetooth-Funktionalität implementieren
                Toast.makeText(this, "Bluetooth-Beacon erkannt: ${result.substring(7)}", Toast.LENGTH_LONG).show()
                finish()
            }
            
            // Fall 4: 4-stellige Session-ID
            result.matches(Regex("[A-Za-z0-9]{4}")) -> {
                Log.d(TAG, "Processing direct session ID: $result")
                // 4-stellige Session-ID
                // Speichere die Session-ID mit eigenem Key
                sharedPreferences.edit()
                    .putString("last_session_id", result)
                    .apply()
                
                // Starte CloudModeLauncherActivity mit der Session-ID
                val intent = Intent(this, CloudModeLauncherActivity::class.java)
                intent.putExtra("SESSION_ID", result)
                startActivity(intent)
                finish()
            }
            
            // Unbekanntes Format
            else -> {
                Log.d(TAG, "Unknown QR code format, trying to extract session ID")
                // Versuche, ob es eine URL mit Session-ID ist
                val sessionId = extractSessionId(result)
                if (sessionId != null) {
                    Log.d(TAG, "Found session ID in unknown format: $sessionId")
                    // Speichere die Session-ID mit eigenem Key
                    sharedPreferences.edit()
                        .putString("last_session_id", sessionId)
                        .apply()
                    
                    val intent = Intent(this, CloudModeLauncherActivity::class.java)
                    intent.putExtra("SESSION_ID", sessionId)
                    startActivity(intent)
                    finish()
                } else {
                    // Wirklich ungültiger QR-Code
                    Log.d(TAG, "Invalid QR code")
                    Toast.makeText(this, "Ungültiger QR-Code: $result", Toast.LENGTH_LONG).show()
                    // Scanner neu starten
                    barcodeView.decodeSingle(object : BarcodeCallback {
                        override fun barcodeResult(result: BarcodeResult) {
                            handleScanResult(result.text)
                        }
                    })
                }
            }
        }
    }
    
    private fun extractSessionId(url: String): String? {
        // Suche nach dem sessionID-Parameter in der URL
        val regex = "[?&]sessionID=([A-Za-z0-9]{4})".toRegex()
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }
    
    private fun showInvalidQrCodeMessage() {
        Toast.makeText(this, "Ungültiger QR-Code. Bitte scannen Sie einen gültigen CBRN-Trainer QR-Code.", Toast.LENGTH_LONG).show()
        // Scanner neu starten
        barcodeView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleScanResult(result.text)
            }
        })
    }
} 