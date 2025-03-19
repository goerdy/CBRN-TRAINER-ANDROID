package de.cbrntrainer

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class CloudSettingsActivity : BaseActivity() {
    
    private lateinit var serverUrlInput: EditText
    private lateinit var sharedPreferences: SharedPreferences
    
    // Gas-Schwellenwerte Konstanten
    companion object {
        // Shared Preferences Keys
        const val PREFS_NAME = "CloudSettings"
        const val URL_KEY = "base_url"
        
        // Default-Werte
        const val DEFAULT_URL = "https://cbrn-trainer.de/c"
        
        // CO (ppm)
        const val CO_A1_DEFAULT = 30.0
        const val CO_A2_DEFAULT = 60.0
        
        // CH4 (%UEG)
        const val CH4_A1_DEFAULT = 20.0
        const val CH4_A2_DEFAULT = 40.0
        
        // CO2 (VOL%)
        const val CO2_A1_DEFAULT = 1.5
        const val CO2_A2_DEFAULT = 3.0
        
        // iBut (ppm)
        const val IBUT_A1_DEFAULT = 45.0
        const val IBUT_A2_DEFAULT = 90.0
        
        // Nona (%UEG)
        const val NONA_A1_DEFAULT = 20.0
        const val NONA_A2_DEFAULT = 40.0
        
        // H2S (ppm)
        const val H2S_A1_DEFAULT = 5.0
        const val H2S_A2_DEFAULT = 10.0
        
        // NH3 (ppm)
        const val NH3_A1_DEFAULT = 20.0
        const val NH3_A2_DEFAULT = 40.0
        
        // O2 (VOL%)
        const val O2_A1_LOW_DEFAULT = 17.0
        const val O2_A1_HIGH_DEFAULT = 23.0
        const val O2_A2_LOW_DEFAULT = 19.0
        const val O2_A2_HIGH_DEFAULT = 24.0
        
        // Dosisleistung (µSv/h)
        const val DOSISLEISTUNG_A1_DEFAULT = 100.0  // 100 µSv/h
        const val DOSISLEISTUNG_A2_DEFAULT = 500.0  // 500 µSv/h
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_settings)
        
        serverUrlInput = findViewById(R.id.serverUrlInput)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Lade gespeicherte URL
        val savedUrl = sharedPreferences.getString(URL_KEY, DEFAULT_URL)
        serverUrlInput.setText(savedUrl)
        
        // Speichern-Button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val url = serverUrlInput.text.toString().trim()
            
            if (url.isEmpty()) {
                Toast.makeText(this, "Bitte geben Sie eine URL ein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // URL speichern
            sharedPreferences.edit()
                .putString(URL_KEY, url)
                .apply()
            
            Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        setupGasThresholds()
    }
    
    private fun setupGasThresholds() {
        // CO
        findViewById<EditText>(R.id.coA1Input).setText(getThreshold("co_a1", CO_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.coA2Input).setText(getThreshold("co_a2", CO_A2_DEFAULT).toString())
        
        // CH4
        findViewById<EditText>(R.id.ch4A1Input).setText(getThreshold("ch4_a1", CH4_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.ch4A2Input).setText(getThreshold("ch4_a2", CH4_A2_DEFAULT).toString())
        
        // CO2
        findViewById<EditText>(R.id.co2A1Input).setText(getThreshold("co2_a1", CO2_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.co2A2Input).setText(getThreshold("co2_a2", CO2_A2_DEFAULT).toString())
        
        // iBut
        findViewById<EditText>(R.id.ibutA1Input).setText(getThreshold("ibut_a1", IBUT_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.ibutA2Input).setText(getThreshold("ibut_a2", IBUT_A2_DEFAULT).toString())
        
        // Nona
        findViewById<EditText>(R.id.nonaA1Input).setText(getThreshold("nona_a1", NONA_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.nonaA2Input).setText(getThreshold("nona_a2", NONA_A2_DEFAULT).toString())
        
        // H2S
        findViewById<EditText>(R.id.h2sA1Input).setText(getThreshold("h2s_a1", H2S_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.h2sA2Input).setText(getThreshold("h2s_a2", H2S_A2_DEFAULT).toString())
        
        // NH3
        findViewById<EditText>(R.id.nh3A1Input).setText(getThreshold("nh3_a1", NH3_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.nh3A2Input).setText(getThreshold("nh3_a2", NH3_A2_DEFAULT).toString())
        
        // O2
        findViewById<EditText>(R.id.o2A1LowInput).setText(getThreshold("o2_a1_low", O2_A1_LOW_DEFAULT).toString())
        findViewById<EditText>(R.id.o2A1HighInput).setText(getThreshold("o2_a1_high", O2_A1_HIGH_DEFAULT).toString())
        findViewById<EditText>(R.id.o2A2LowInput).setText(getThreshold("o2_a2_low", O2_A2_LOW_DEFAULT).toString())
        findViewById<EditText>(R.id.o2A2HighInput).setText(getThreshold("o2_a2_high", O2_A2_HIGH_DEFAULT).toString())
        
        // Dosisleistung
        findViewById<EditText>(R.id.dosisleistungA1Input).setText(
            getThreshold("dosisleistung_a1", DOSISLEISTUNG_A1_DEFAULT).toString())
        findViewById<EditText>(R.id.dosisleistungA2Input).setText(
            getThreshold("dosisleistung_a2", DOSISLEISTUNG_A2_DEFAULT).toString())
    }
    
    private fun getThreshold(key: String, defaultValue: Double): Double {
        return sharedPreferences.getFloat(key, defaultValue.toFloat()).toDouble()
    }
} 