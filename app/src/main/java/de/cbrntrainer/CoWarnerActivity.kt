package de.cbrntrainer

import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import android.widget.Toast
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
        private const val TAG = "CoWarnerActivity"
        private const val ALARM_DURATION = 500L  // Ton-Dauer in ms
        private const val A1_INTERVAL = 2000L    // A1-Alarm alle 2 Sekunden
        private const val A2_INTERVAL = 1000L    // A2-Alarm jede Sekunde
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_co_warner)

        // Vollbildmodus für Messgeräte
        hideSystemUI()

        setupWebView()
        setupSharedPreferences()
        setupToneGenerator()

        val sessionId = intent.getStringExtra("SESSION_ID")
        if (sessionId.isNullOrEmpty()) {
            Log.e(TAG, "Session ID is null or empty")
            Toast.makeText(this, "Fehler: Keine Session-ID erhalten", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d(TAG, "Starting with session ID: $sessionId")
        startDataStream(sessionId)
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WebView page loaded: $url")
                // Initial value setzen
                updateUI(0.0)
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error: $errorCode - $description")
                Toast.makeText(this@CoWarnerActivity, "WebView Fehler: $description", Toast.LENGTH_LONG).show()
            }
        }

        try {
            webView.loadUrl("file:///android_asset/CO.html")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading WebView", e)
            Toast.makeText(this, "Fehler beim Laden der Anzeige: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(CloudSettingsActivity.PREFS_NAME, MODE_PRIVATE)
    }

    private fun setupToneGenerator() {
        try {
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ToneGenerator", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
        toneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ToneGenerator", e)
        }
        stopAlarms()
    }

    private fun startDataStream(sessionId: String) {
        lifecycleScope.launch {
            try {
            CloudRepository.getInstance(this@CoWarnerActivity)
                .getDeviceDataFlow(sessionId)
                .collect { result ->
                        // Prüfe, ob die Activity noch aktiv ist
                        if (isFinishing || isDestroyed) return@collect

                    result.onSuccess { response ->
                            try {
                                Log.d(TAG, "Received CO data: ${response.data.co}")
                        updateUI(response.data.co)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing response data", e)
                                updateUI(0.0) // Fallback to safe value
                            }
                    }.onFailure { error ->
                            Log.e(TAG, "API error", error)
                        stopAlarms()
                            // Nur Fehlermeldung zeigen, wenn Activity noch aktiv ist
                            if (!isFinishing && !isDestroyed) {
                                runOnUiThread {
                                    Toast.makeText(this@CoWarnerActivity,
                                        "Verbindungsfehler: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in data stream", e)
                // Nur Fehlermeldung zeigen, wenn Activity noch aktiv ist
                if (!isFinishing && !isDestroyed) {
                    runOnUiThread {
                        Toast.makeText(this@CoWarnerActivity,
                            "Fehler im Datenstrom: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    }
                }
        }
    }

    private fun updateUI(value: Double) {
        try {
            Log.d(TAG, "Updating UI with CO value: $value")

        // Update WebView
            runOnUiThread {
                try {
        webView.evaluateJavascript(
            "javascript:updateValue('${value.toInt()}')",
                        { result ->
                            Log.d(TAG, "JavaScript result: $result")
                        }
        )
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating WebView", e)
                }
            }

        // Alarme prüfen
        val a1 = sharedPreferences.getFloat("co_a1", CloudSettingsActivity.CO_A1_DEFAULT.toFloat()).toDouble()
        val a2 = sharedPreferences.getFloat("co_a2", CloudSettingsActivity.CO_A2_DEFAULT.toFloat()).toDouble()

        val alarmLevel = when {
            value >= a2 -> 2
            value >= a1 -> 1
            else -> 0
        }

        handleAlarm(alarmLevel)
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateUI", e)
        }
    }

    private fun handleAlarm(alarmLevel: Int) {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error handling alarm", e)
        }
    }

    private fun playA1Alarm() {
        try {
        handler.post(object : Runnable {
            override fun run() {
                if (isA1AlarmActive) {
                        try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ALARM_DURATION.toInt())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error playing A1 alarm", e)
                        }
                    handler.postDelayed(this, A1_INTERVAL)
                }
            }
        })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up A1 alarm", e)
        }
    }

    private fun playA2Alarm() {
        try {
        handler.post(object : Runnable {
            override fun run() {
                if (isA2AlarmActive) {
                        try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, ALARM_DURATION.toInt())
                        } catch (e: Exception) {
                            Log.e(TAG, "Error playing A2 alarm", e)
                        }
                    handler.postDelayed(this, A2_INTERVAL)
                }
            }
        })
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up A2 alarm", e)
        }
    }

    private fun stopAlarms() {
        try {
        isA1AlarmActive = false
        isA2AlarmActive = false
        handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarms", e)
        }
    }
}