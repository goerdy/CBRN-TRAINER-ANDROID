package de.cbrntrainer

import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.JavascriptInterface
import androidx.lifecycle.lifecycleScope
import de.cbrntrainer.repository.CloudRepository
import kotlinx.coroutines.launch

class CloudDosisleistungsmessActivity : BaseActivity() {
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isA1AlarmActive = false
    private var isA2AlarmActive = false
    
    companion object {
        private const val ALARM_DURATION = 500L
        private const val A1_INTERVAL = 2000L
        private const val A2_INTERVAL = 1000L
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_dosisleistungsmess)
        
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/meter.html")
        
        sharedPreferences = getSharedPreferences(CloudSettingsActivity.PREFS_NAME, MODE_PRIVATE)
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        
        val sessionId = intent.getStringExtra("SESSION_ID") ?: return finish()
        startDataStream(sessionId)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        stopAlarms()
    }
    
    private fun startDataStream(sessionId: String) {
        lifecycleScope.launch {
            CloudRepository.getInstance(this@CloudDosisleistungsmessActivity)
                .getDeviceDataFlow(sessionId)
                .collect { result ->
                    result.onSuccess { response ->
                        updateUI(response.data.dosisleistung)
                    }.onFailure { error ->
                        stopAlarms()
                    }
                }
        }
    }
    
    private fun updateUI(value: Double) {
        // Formatiere den Wert für die Anzeige
        val formattedValue = formatValue(value)
        
        // Update WebView
        webView.evaluateJavascript(
            "javascript:updateValue('${formattedValue.first}', '${formattedValue.second}')",
            null
        )
        
        // Alarme prüfen
        val a1 = sharedPreferences.getFloat("dosisleistung_a1", CloudSettingsActivity.DOSISLEISTUNG_A1_DEFAULT.toFloat()).toDouble()
        val a2 = sharedPreferences.getFloat("dosisleistung_a2", CloudSettingsActivity.DOSISLEISTUNG_A2_DEFAULT.toFloat()).toDouble()
        
        val alarmLevel = when {
            value >= a2 -> 2
            value >= a1 -> 1
            else -> 0
        }
        
        handleAlarm(alarmLevel)
    }
    
    private fun formatValue(value: Double): Pair<String, String> {
        val microSv = value * 1000000 // Konvertiere in µSv/h
        
        return when {
            microSv < 999 -> {
                Pair(String.format("%.2f", microSv), "µSv/h")
            }
            microSv < 999000 -> {
                Pair(String.format("%.2f", microSv / 1000), "mSv/h")
            }
            else -> {
                Pair(String.format("%.2f", microSv / 1000000), "Sv/h")
            }
        }
    }
    
    private fun handleAlarm(alarmLevel: Int) {
        when (alarmLevel) {
            2 -> {
                if (!isA2AlarmActive) {
                    stopAlarms()
                    isA2AlarmActive = true
                    playA2Alarm()
                }
            }
            1 -> {
                if (!isA1AlarmActive) {
                    stopAlarms()
                    isA1AlarmActive = true
                    playA1Alarm()
                }
            }
            else -> {
                stopAlarms()
            }
        }
    }
    
    private fun playA1Alarm() {
        handler.post(object : Runnable {
            override fun run() {
                if (isA1AlarmActive) {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ALARM_DURATION.toInt())
                    handler.postDelayed(this, A1_INTERVAL)
                }
            }
        })
    }
    
    private fun playA2Alarm() {
        handler.post(object : Runnable {
            override fun run() {
                if (isA2AlarmActive) {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, ALARM_DURATION.toInt())
                    handler.postDelayed(this, A2_INTERVAL)
                }
            }
        })
    }
    
    private fun stopAlarms() {
        isA1AlarmActive = false
        isA2AlarmActive = false
        handler.removeCallbacksAndMessages(null)
    }
} 