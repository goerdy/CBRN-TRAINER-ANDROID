package de.cbrntrainer

import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.lifecycle.lifecycleScope
import de.cbrntrainer.repository.CloudRepository
import de.cbrntrainer.api.MeasurementData
import kotlinx.coroutines.launch

class MultiGasActivity : BaseActivity() {
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
        setContentView(R.layout.activity_multigas)
        
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/multigas.html")
        
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
            CloudRepository.getInstance(this@MultiGasActivity)
                .getDeviceDataFlow(sessionId)
                .collect { result ->
                    result.onSuccess { response ->
                        updateUI(response.data)
                    }
                }
        }
    }
    
    private fun updateUI(data: MeasurementData) {
        // Formatiere die Daten als JSON für JavaScript
        val json = """
            {
                "ibut": ${data.ibut},
                "nona": ${data.nona},
                "o2": ${data.o2},
                "h2s": ${data.h2s},
                "co": ${data.co},
                "nh3": ${data.nh3}
            }
        """.trimIndent()
        
        // Update WebView
        webView.evaluateJavascript(
            "javascript:updateValues($json)",
            null
        )
        
        // Alarme prüfen
        val alarmLevel = checkAlarmLevel(data)
        handleAlarm(alarmLevel)
    }
    
    private fun checkAlarmLevel(data: MeasurementData): Int {
        // O2-Alarme
        if (data.o2 < 17 || data.o2 > 23) return 2
        if (data.o2 < 19.5 || data.o2 > 21.5) return 1
        
        // Gas-Alarme aus den Einstellungen
        val coA2 = sharedPreferences.getFloat("co_a2", CloudSettingsActivity.CO_A2_DEFAULT.toFloat())
        if (data.co >= coA2) return 2
        
        val coA1 = sharedPreferences.getFloat("co_a1", CloudSettingsActivity.CO_A1_DEFAULT.toFloat())
        if (data.co >= coA1) return 1
        
        return 0
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