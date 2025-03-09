package de.cbrntrainer

import android.os.Bundle
import android.webkit.WebView
import androidx.lifecycle.lifecycleScope
import de.cbrntrainer.repository.CloudRepository
import de.cbrntrainer.api.MeasurementData
import kotlinx.coroutines.launch

class TestDeviceActivity : BaseActivity() {
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_device)
        
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/test_device.html")
        
        val sessionId = intent.getStringExtra("SESSION_ID") ?: return finish()
        startDataStream(sessionId)
    }
    
    private fun startDataStream(sessionId: String) {
        lifecycleScope.launch {
            CloudRepository.getInstance(this@TestDeviceActivity)
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
                "sessionId": "${data.sessionId}",
                "dosisleistung": ${data.dosisleistung},
                "dosis": ${data.dosis},
                "co": ${data.co},
                "ch4": ${data.ch4},
                "co2": ${data.co2},
                "o2": ${data.o2},
                "ibut": ${data.ibut},
                "nona": ${data.nona},
                "h2s": ${data.h2s},
                "nh3": ${data.nh3},
                "distance": ${data.distance},
                "sourceStrength": ${data.sourceStrength}
            }
        """.trimIndent()
        
        // Update WebView
        webView.evaluateJavascript(
            "javascript:updateValues($json)",
            null
        )
    }
} 