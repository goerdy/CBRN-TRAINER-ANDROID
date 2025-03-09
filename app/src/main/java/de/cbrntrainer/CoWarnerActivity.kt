package de.cbrntrainer

import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.lifecycle.lifecycleScope
import de.cbrntrainer.repository.CloudRepository
import kotlinx.coroutines.launch

class CoWarnerActivity : BaseActivity() {
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isA1AlarmActive = false
    private var isA2AlarmActive = false
    
    companion object {
        private const val ALARM_DURATION = 500L  // Ton-Dauer in ms
        private const val A1_INTERVAL = 2000L    // A1-Alarm alle 2 Sekunden
        private const val A2_INTERVAL = 1000L    // A2-Alarm jede Sekunde
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_co_warner)
        
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/CO.html")
        
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
            CloudRepository.getInstance(this@CoWarnerActivity)
                .getDeviceDataFlow(sessionId)
                .collect { result ->
                    result.onSuccess { response ->
                        updateUI(response.data.co)
                    }.onFailure { error ->
                        stopAlarms()
                    }
                }
        }
    }
    
    private fun updateUI(value: Double) {
        // Update WebView
        webView.evaluateJavascript(
            "javascript:updateValue('${value.toInt()}')",
            null
        )
        
        // Alarme prüfen
        val a1 = sharedPreferences.getFloat("co_a1", CloudSettingsActivity.CO_A1_DEFAULT.toFloat()).toDouble()
        val a2 = sharedPreferences.getFloat("co_a2", CloudSettingsActivity.CO_A2_DEFAULT.toFloat()).toDouble()
        
        val alarmLevel = when {
            value >= a2 -> 2
            value >= a1 -> 1
            else -> 0
        }
        
        handleAlarm(alarmLevel)
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