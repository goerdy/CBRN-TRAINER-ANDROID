package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vollbild-Modus (Immersive Mode)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Verarbeite eingehende Intents
        handleIntent(intent)

        // QR-Code Scanner Button
        findViewById<Button>(R.id.scanQrButton).setOnClickListener {
            startActivity(Intent(this, QrScannerActivity::class.java))
        }

        // Web-Modus Button
        findViewById<Button>(R.id.webModeButton).setOnClickListener {
            startActivity(Intent(this, SessionInputActivity::class.java))
        }

        // Dosisleistungsmess Button
        findViewById<Button>(R.id.dosisleistungsmessButton).setOnClickListener {
            startActivity(Intent(this, DosisleistungsmessActivity::class.java))
        }

        // Magnet-Modus Button
        findViewById<Button>(R.id.magnetButton).setOnClickListener {
            startActivity(Intent(this, MagnetModeActivity::class.java))
        }

        // Einstellungen Button
        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Info Button
        findViewById<Button>(R.id.infoButton).setOnClickListener {
            startActivity(Intent(this, InstructionsActivity::class.java))
        }

        // Exit Button
        findViewById<Button>(R.id.exitButton).setOnClickListener {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        
        if (Intent.ACTION_VIEW == action && data != null) {
            val scheme = data.scheme
            
            if (scheme == "cbrn-trainer") {
                // Extrahiere den Pfad und die Parameter
                val host = data.host
                val path = data.path
                
                when {
                    // Beispiel: cbrn-trainer://session/1234
                    host == "session" && !path.isNullOrEmpty() -> {
                        val sessionId = path.substring(1) // Entferne den führenden "/"
                        val intent = Intent(this, WebViewActivity::class.java)
                        intent.putExtra("SESSION_ID", sessionId)
                        startActivity(intent)
                    }
                    
                    // Beispiel: cbrn-trainer://bluetooth/AA:BB:CC:DD:EE:FF
                    host == "bluetooth" && !path.isNullOrEmpty() -> {
                        val beaconAddress = path.substring(1)
                        val intent = Intent(this, BluetoothModeActivity::class.java)
                        intent.putExtra("BEACON_ADDRESS", beaconAddress)
                        startActivity(intent)
                    }
                    
                    // Fallback
                    else -> {
                        Toast.makeText(this, "Unbekannter CBRN-Trainer Link: $data", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
} 