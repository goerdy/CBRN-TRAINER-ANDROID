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
        private const val TAG = "MultiGasActivity"
        private const val ALARM_DURATION = 500L
        private const val A1_INTERVAL = 2000L
        private const val A2_INTERVAL = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_gas)

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
                // Initial values setzen
                updateUI(MeasurementData(
                    sessionId = "",
                    dosisleistung = 0.0,
                    dosis = 0.0,
                    co = 0.0,
                    ch4 = 0.0,
                    co2 = 0.0,
                    o2 = 21.0,
                    ibut = 0.0,
                    nona = 0.0,
                    h2s = 0.0,
                    nh3 = 0.0,
                    distance = 0.0,
                    sourceStrength = 0.0,
                    teletector = false,
                    cover = false
                ))
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error: $errorCode - $description")
                Toast.makeText(this@MultiGasActivity, "WebView Fehler: $description", Toast.LENGTH_LONG).show()
            }
        }

        try {
            webView.loadUrl("file:///android_asset/multigas.html")
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
            CloudRepository.getInstance(this@MultiGasActivity)
                .getDeviceDataFlow(sessionId)
                .collect { result ->
                        // Prüfe, ob die Activity noch aktiv ist
                        if (isFinishing || isDestroyed) return@collect

                    result.onSuccess { response ->
                            try {
                                Log.d(TAG, "Received data: ${response.data}")
                        updateUI(response.data)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing response data", e)
                                // Fallback to safe values
                                updateUI(MeasurementData(
                                    sessionId = sessionId,
                                    dosisleistung = 0.0,
                                    dosis = 0.0,
                                    co = 0.0,
                                    ch4 = 0.0,
                                    co2 = 0.0,
                                    o2 = 21.0,
                                    ibut = 0.0,
                                    nona = 0.0,
                                    h2s = 0.0,
                                    nh3 = 0.0,
                                    distance = 0.0,
                                    sourceStrength = 0.0,
                                    teletector = false,
                                    cover = false
                                ))
                            }
                        }.onFailure { error ->
                            Log.e(TAG, "API error", error)
                            stopAlarms()
                            // Nur Fehlermeldung zeigen, wenn Activity noch aktiv ist
                            if (!isFinishing && !isDestroyed) {
                                runOnUiThread {
                                    Toast.makeText(this@MultiGasActivity,
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
                        Toast.makeText(this@MultiGasActivity,
                            "Fehler im Datenstrom: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    }
                }
        }
    }

    private fun updateUI(data: MeasurementData) {
        try {
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

            Log.d(TAG, "Updating UI with data: $json")

        // Update WebView
            runOnUiThread {
                try {
        webView.evaluateJavascript(
            "javascript:updateValues($json)",
                        { result ->
                            Log.d(TAG, "JavaScript result: $result")
                        }
        )
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating WebView", e)
                }
            }

        // Alarme prüfen
        val alarmLevel = checkAlarmLevel(data)
        handleAlarm(alarmLevel)
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateUI", e)
        }
    }

    private fun checkAlarmLevel(data: MeasurementData): Int {
        return try {
        // O2-Alarme
        if (data.o2 < 17 || data.o2 > 23) return 2
        if (data.o2 < 19.5 || data.o2 > 21.5) return 1

        // Gas-Alarme aus den Einstellungen
        val coA2 = sharedPreferences.getFloat("co_a2", CloudSettingsActivity.CO_A2_DEFAULT.toFloat())
        if (data.co >= coA2) return 2

        val coA1 = sharedPreferences.getFloat("co_a1", CloudSettingsActivity.CO_A1_DEFAULT.toFloat())
        if (data.co >= coA1) return 1

            0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking alarm level", e)
            0
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