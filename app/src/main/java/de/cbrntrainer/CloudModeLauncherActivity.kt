package de.cbrntrainer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.SharedPreferences

class CloudModeLauncherActivity : BaseActivity() {
    
    private lateinit var sessionIdInput: EditText
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        const val LAST_SESSION_ID = "last_session_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_mode_launcher)
        
        sessionIdInput = findViewById(R.id.sessionIdInput)
        sharedPreferences = getSharedPreferences(CloudSettingsActivity.PREFS_NAME, MODE_PRIVATE)
        
        // Letzte Session ID laden
        val lastSessionId = sharedPreferences.getString(LAST_SESSION_ID, "")
        sessionIdInput.setText(lastSessionId)
        
        // Messgerät Button (vorher Test-Messgerät)
        findViewById<Button>(R.id.testDeviceButton).setOnClickListener {
            handleDeviceButton(TestDeviceActivity::class.java)
        }
        
        // CO-Warner Button
        findViewById<Button>(R.id.coWarnerButton).setOnClickListener {
            handleDeviceButton(CoWarnerActivity::class.java)
        }
        
        // Dosisleistungsmess Button
        findViewById<Button>(R.id.radiationMeterButton).setOnClickListener {
            handleDeviceButton(CloudDosisleistungsmessActivity::class.java)
        }
        
        // Multi-Gas Button
        findViewById<Button>(R.id.multiGasButton).setOnClickListener {
            handleDeviceButton(MultiGasActivity::class.java)
        }
        
        // Zurück Button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Die anderen Geräte-Buttons werden später implementiert
    }
    
    private fun handleDeviceButton(activityClass: Class<*>) {
        val sessionId = sessionIdInput.text.toString()
        if (sessionId.length == 4) {
            // Session ID speichern
            sharedPreferences.edit()
                .putString(LAST_SESSION_ID, sessionId)
                .apply()
            
            // Activity starten
            val intent = Intent(this, activityClass)
            intent.putExtra("SESSION_ID", sessionId)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Bitte geben Sie eine 4-stellige Session ID ein", Toast.LENGTH_SHORT).show()
        }
    }
} 