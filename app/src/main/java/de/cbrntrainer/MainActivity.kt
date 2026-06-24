package de.cbrntrainer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prüfe, ob Onboarding bereits abgeschlossen wurde
        val onboardingCompleted = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("onboarding_completed", false)

        if (!onboardingCompleted) {
            val onboardingIntent = Intent(this, OnboardingActivity::class.java)
            if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
                onboardingIntent.putExtra(OnboardingActivity.EXTRA_PENDING_ACTION, intent.action)
                onboardingIntent.putExtra(OnboardingActivity.EXTRA_PENDING_DATA, intent.dataString)
            }

            // Starte Onboarding
            startActivity(onboardingIntent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Stelle sicher, dass die Systemleiste sichtbar ist
        useFullscreen = false
        showSystemUI()

        // Verarbeite eingehende Intents
        handleIntent(intent)

        // QR-Code Scanner Button
        findViewById<Button>(R.id.scanQrButton).setOnClickListener {
            startActivity(Intent(this, QrScannerActivity::class.java))
        }

        // Web-Modus Button (jetzt Cloud-Modus)
        findViewById<Button>(R.id.webButton).setOnClickListener {
            startActivity(Intent(this, CloudModeLauncherActivity::class.java))
        }

        // Dosisleistungsmess Button
        findViewById<Button>(R.id.dosisleistungsmessButton).setOnClickListener {
            startActivity(Intent(this, DosisleistungsmessActivity::class.java))
        }

        // Dosisleistungswarner Button
        findViewById<Button>(R.id.dlWarnerButton).setOnClickListener {
            startActivity(Intent(this, BluetoothDLWarnActivity::class.java))
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

        // Optional: Programmatischer Zugriff auf das ImageView
        findViewById<ImageView>(R.id.mainImage)
        // Hier kannst du weitere Anpassungen am ImageView vornehmen
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
                        val launchIntent = Intent(this, WebViewActivity::class.java)
                        launchIntent.putExtra("SESSION_ID", sessionId)
                        startActivity(launchIntent)
                    }
                    
                    // Beispiel: cbrn-trainer://bluetooth/AA:BB:CC:DD:EE:FF
                    host == "bluetooth" && !path.isNullOrEmpty() -> {
                        val beaconAddress = path.substring(1)
                        val launchIntent = Intent(this, BluetoothModeActivity::class.java)
                        launchIntent.putExtra("BEACON_ADDRESS", beaconAddress)
                        startActivity(launchIntent)
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
