package de.cbrntrainer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.WebView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import kotlin.math.sqrt

class MagnetModeActivity : BaseActivity(), SensorEventListener {
    
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private lateinit var meterWebView: WebView
    
    private var nullEffektWert: Float = 0f
    private var isNullEffektMessung = false
    private var nullEffektValues = mutableListOf<Float>()
    private var showNetto = false
    private var countdownTimer: CountDownTimer? = null
    private var remainingTime: Long = 0  // Speichert die verbleibende Zeit
    private val recentValues = mutableListOf<Float>()
    private val WINDOW_SIZE = 60  // Anzahl der Messwerte für Mittelwertbildung
    private val UPDATE_INTERVAL = 333L  // Minimaler Abstand zwischen Updates in ms (3Hz)
    private var lastUpdateTime = 0L
    
    private lateinit var toneGenerator: ToneGenerator
    private lateinit var vibrator: Vibrator
    private var lastClickTime: Long = 0
    private val MIN_CLICK_INTERVAL = 5L  // Kürzerer Mindestabstand zwischen Klicks
    private val MAX_IPS = 30f
    private var isWarningToneActive = false
    private var isMuted = false
    private val CLICK_RATE_MULTIPLIER = 5f  // Erhöht die Klickrate
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magnet_mode)
        
        // Initialisiere WebView
        meterWebView = findViewById(R.id.meterWebView)
        meterWebView.settings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
        }
        meterWebView.webChromeClient = android.webkit.WebChromeClient()
        meterWebView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Initial Update nach dem Laden
                updateDisplay(0.0, "---")
            }
        }
        meterWebView.loadUrl("file:///android_asset/magnet_meter.html")
        
        // Nulleffekt Button
        findViewById<Button>(R.id.nullEffektButton).setOnClickListener {
            startNullEffektMessung()
        }
        
        // Brutto/Netto Button
        findViewById<Button>(R.id.bruttoNettoButton).setOnClickListener {
            showNetto = !showNetto
            updateBruttoNettoButton()
        }
        
        // Initialisiere Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        if (magnetometer == null) {
            // Gerät hat keinen Magnetometer
            // Zeige Fehlermeldung im WebView
            updateDisplay(0.0, "ERROR|brutto")
            return
        }
        
        // Initialisiere ToneGenerator und Vibrator
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        // Mute Button
        findViewById<Button>(R.id.muteButton).setOnClickListener {
            isMuted = !isMuted
            updateMuteButton()
            if (isMuted && isWarningToneActive) {
                toneGenerator.stopTone()
                vibrator.cancel()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        magnetometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    
    private fun startNullEffektMessung() {
        isNullEffektMessung = true
        nullEffektValues.clear()
        findViewById<Button>(R.id.nullEffektButton).isEnabled = false
        
        countdownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
            }
            
            override fun onFinish() {
                remainingTime = 0
                finishNullEffektMessung()
            }
        }.start()
    }
    
    private fun finishNullEffektMessung() {
        isNullEffektMessung = false
        findViewById<Button>(R.id.nullEffektButton).isEnabled = true
        
        if (nullEffektValues.isNotEmpty()) {
            nullEffektWert = nullEffektValues.average().toFloat()
        }
    }
    
    private fun updateBruttoNettoButton() {
        val button = findViewById<Button>(R.id.bruttoNettoButton)
        button.text = if (showNetto) "Brutto" else "Netto"
    }
    
    private fun updateMuteButton() {
        val button = findViewById<Button>(R.id.muteButton)
        button.text = if (isMuted) "Ton an" else "Ton aus"
    }
    
    private fun playClickIfNeeded(intensity: Float) {
        val currentTime = System.currentTimeMillis()
        
        // Prüfe auf Überschreitung des Maximums
        if (intensity >= MAX_IPS) {
            if (!isWarningToneActive) {
                if (!isMuted) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_A, -1)
                }
                isWarningToneActive = true
                val pattern = longArrayOf(0, 500, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
            return
        } else if (isWarningToneActive) {
            if (!isMuted) {
                toneGenerator.stopTone()
            }
            isWarningToneActive = false
            vibrator.cancel()
        }
        
        // Zufallsbasierte Klicks entsprechend der IPS
        if (currentTime - lastClickTime >= MIN_CLICK_INTERVAL) {
            // Erhöhte Wahrscheinlichkeit für Klicks
            val probability = (intensity * CLICK_RATE_MULTIPLIER) / (1000f / MIN_CLICK_INTERVAL)
            if (Math.random() < probability) {
                if (!isMuted) {
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_S, 1)
                }
                lastClickTime = currentTime
            }
        }
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            val magnitude = sqrt(x * x + y * y + z * z)
            val ips = magnitude / 3f
            
            // Sammle Werte während der Nulleffektmessung
            if (isNullEffektMessung) {
                nullEffektValues.add(ips)
            }
            
            // Füge neuen Wert zum Sliding Window hinzu
            recentValues.add(ips)
            if (recentValues.size > WINDOW_SIZE) {
                recentValues.removeAt(0)  // Entferne ältesten Wert
            }
            
            // Prüfe, ob ein Update der Anzeige fällig ist
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
                return
            }
            lastUpdateTime = currentTime
            
            // Berechne Durchschnitt der letzten Werte
            val averageValue = recentValues.average().toFloat()
            
            // Zeige Brutto- oder Nettowert an
            val displayValue = if (showNetto) {
                (averageValue - nullEffektWert).coerceAtLeast(0f)
            } else {
                averageValue
            }
            
            // Aktualisiere UI
            if (isNullEffektMessung) {
                // Während Nulleffektmessung: Zeige Countdown und aktuelle Rate
                val seconds = remainingTime / 1000
                val countdownText = "Nulleffektmessung: $seconds s"
                updateDisplay(displayValue.toDouble(), countdownText + "|" + (if (showNetto) "netto" else "brutto"))
            } else {
                // Normaler Betrieb: Zeige Messwert und Nullrate
                val nullrateText = if (nullEffektWert > 0f) String.format("%.1f", nullEffektWert) else "---"
                updateDisplay(displayValue.toDouble(), nullrateText + "|" + (if (showNetto) "netto" else "brutto"))
            }
            
            // Spiele Klick-Sound basierend auf aktuellem Wert
            playClickIfNeeded(displayValue)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nicht benötigt
    }
    
    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        if (isWarningToneActive) {
            toneGenerator.stopTone()
            vibrator.cancel()
        }
        toneGenerator.release()
    }
    
    private fun updateDisplay(value1: Double, value2: String) {
        runOnUiThread {
            val formattedValue1 = String.format(java.util.Locale.US, "%.1f", value1)
            meterWebView.evaluateJavascript(
                "javascript:updateValue('$formattedValue1', '$value2')",
                null
            )
        }
    }
} 