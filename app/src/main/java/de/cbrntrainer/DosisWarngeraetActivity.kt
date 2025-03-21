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

class DosisWarngeraetActivity : BaseActivity() {
    private lateinit var webView: WebView
    private lateinit var sharedPreferences: android.content.SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dosis_warngeraet)
        
        // Vollbildmodus für Messgeräte
        hideSystemUI()
        
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("file:///android_asset/dosiswarngeraet.html")
        
        sharedPreferences = getSharedPreferences(CloudSettingsActivity.PREFS_NAME, MODE_PRIVATE)
        
        val sessionId = intent.getStringExtra("SESSION_ID") ?: return finish()
        startDataStream(sessionId)
    }
    
    private fun startDataStream(sessionId: String) {
        lifecycleScope.launch {
            CloudRepository.getInstance(this@DosisWarngeraetActivity)
                .getDeviceDataFlow(sessionId)
                .collect { result ->
                    result.onSuccess { response ->
                        updateUI(response.data.dosis)
                    }
                }
        }
    }
    
    private fun updateUI(value: Double) {
        // Update WebView mit dem Wert
        webView.evaluateJavascript(
            "javascript:updateValue($value)",
            null
        )
    }
    
    override fun onPause() {
        super.onPause()
        // Stoppe das Gerät, wenn die Activity pausiert wird
        webView.evaluateJavascript("javascript:stopDevice()", null)
    }
    
    override fun onDestroy() {
        // Stoppe das Gerät, wenn die Activity zerstört wird
        webView.evaluateJavascript("javascript:stopDevice()", null)
        super.onDestroy()
    }
} 