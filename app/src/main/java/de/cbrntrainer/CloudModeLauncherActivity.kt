package de.cbrntrainer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.SharedPreferences

class CloudModeLauncherActivity : BaseActivity() {
    private val TAG = "CloudModeLauncherActivity"
    
    private lateinit var sessionIdInput: EditText
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        const val LAST_SESSION_ID = "last_session_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_mode_launcher)
        
        // Session-ID aus Intent holen
        val sessionId = intent.getStringExtra("SESSION_ID")
        Log.d(TAG, "Received session ID: $sessionId")
        
        // Session-ID in das Eingabefeld eintragen, wenn vorhanden
        sessionIdInput = findViewById(R.id.sessionIdInput)
        if (!sessionId.isNullOrEmpty()) {
            sessionIdInput.setText(sessionId)
        } else {
            // Versuche, die letzte Session-ID aus den SharedPreferences zu laden
            val sharedPreferences = getSharedPreferences(CloudSettingsActivity.PREFS_NAME, MODE_PRIVATE)
            val lastSessionId = sharedPreferences.getString("last_session_id", "")
            if (!lastSessionId.isNullOrEmpty()) {
                sessionIdInput.setText(lastSessionId)
            }
        }
        
        // Geräte-Buttons einrichten
        setupDeviceButtons()
    }
    
    private fun setupDeviceButtons() {
        // Test-Messgerät
        findViewById<Button>(R.id.testDeviceButton).setOnClickListener {
            launchDeviceActivity(TestDeviceActivity::class.java, sessionIdInput.text.toString())
        }
        
        // CO-Warner
        findViewById<Button>(R.id.coWarnerButton).setOnClickListener {
            launchDeviceActivity(CoWarnerActivity::class.java, sessionIdInput.text.toString())
        }
        
        // Multi-Gaswarngerät
        findViewById<Button>(R.id.multiGasButton).setOnClickListener {
            launchDeviceActivity(MultiGasActivity::class.java, sessionIdInput.text.toString())
        }
        
        // Dosisleistungsmessgerät
        findViewById<Button>(R.id.radiationMeterButton).setOnClickListener {
            launchDeviceActivity(CloudDosisleistungsmessActivity::class.java, sessionIdInput.text.toString())
        }
        
        // Dosisleistungswarngerät
        findViewById<Button>(R.id.radiationWarnerButton).setOnClickListener {
            launchDeviceActivity(DlWarnerActivity::class.java, sessionIdInput.text.toString())
        }
        
        // Dosiswarngerät
        findViewById<Button>(R.id.doseWarnerButton).setOnClickListener {
            launchDeviceActivity(DosisWarngeraetActivity::class.java, sessionIdInput.text.toString())
        }
        
        // Weitere Buttons...
    }
    
    private fun launchDeviceActivity(activityClass: Class<*>, sessionId: String) {
        if (sessionId.isEmpty() || !sessionId.matches(Regex("[A-Za-z0-9]{4}"))) {
            Toast.makeText(this, "Bitte geben Sie eine gültige Session-ID ein (4 Zeichen)", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Session-ID speichern
        val sharedPreferences = getSharedPreferences(CloudSettingsActivity.PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit().putString("last_session_id", sessionId).apply()
        
        // Activity starten
        val intent = Intent(this, activityClass)
        intent.putExtra("SESSION_ID", sessionId)
        startActivity(intent)
    }
} 