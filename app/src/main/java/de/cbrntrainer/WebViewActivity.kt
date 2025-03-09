package de.cbrntrainer

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import de.cbrntrainer.R

class WebViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val sessionId = intent.getStringExtra("SESSION_ID") ?: ""
        val webView = findViewById<WebView>(R.id.webView)
        
        // WebView-Einstellungen
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        
        // Lade die URL mit der Session-ID
        val url = "https://cbrn-trainer.de/c/TRAINEE-APP.php?sessionID=$sessionId"
        webView.loadUrl(url)
    }
    
    // Ermöglicht die Verwendung des Zurück-Buttons im Browser
    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webView)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
} 