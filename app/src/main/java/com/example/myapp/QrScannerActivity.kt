package com.example.myapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class QrScannerActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Starte den Scanner direkt beim Öffnen der Activity
        startQrScanner()
    }
    
    private fun startQrScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("QR-Code scannen")
        integrator.setCameraId(0)  // Verwende die Rückkamera
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.lockOrientation()
        integrator.initiateScan()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        
        if (result != null) {
            if (result.contents == null) {
                // Scan wurde abgebrochen
                Toast.makeText(this, "Scan abgebrochen", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Scan war erfolgreich - verarbeite den Inhalt
                processQrContent(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    
    private fun processQrContent(content: String) {
        when {
            // Fall 1: Web-Link mit TRAINEE.php
            content.startsWith("https://pq5.de/CBRN-TRAINER/TRAINEE.php") -> {
                // Extrahiere die Session-ID aus dem Link
                val sessionId = extractSessionId(content)
                if (sessionId != null) {
                    // Starte WebView mit der Session-ID
                    val intent = Intent(this, WebViewActivity::class.java)
                    intent.putExtra("SESSION_ID", sessionId)
                    startActivity(intent)
                    finish()
                } else {
                    showInvalidQrCodeMessage()
                }
            }
            
            // Fall 2: Bluetooth-Beacon-Format (Schema noch zu definieren)
            content.startsWith("cbrn-bt:") -> {
                // Hier später die Bluetooth-Funktionalität implementieren
                Toast.makeText(this, "Bluetooth-Beacon erkannt: ${content.substring(7)}", Toast.LENGTH_LONG).show()
                finish()
            }
            
            // Prüfen, ob es sich um eine CBRN-Trainer-URL handelt
            content.startsWith("cbrn-trainer://") -> {
                // URL an die MainActivity zur Verarbeitung weiterleiten
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                intent.setPackage(packageName) // Stellt sicher, dass unsere App die URL verarbeitet
                startActivity(intent)
                finish()
                return
            }
            
            // Ungültiger QR-Code
            else -> {
                showInvalidQrCodeMessage()
            }
        }
    }
    
    private fun extractSessionId(url: String): String? {
        // Suche nach dem sessionID-Parameter in der URL
        val regex = "sessionID=([A-Za-z0-9]{4})".toRegex()
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.get(1)
    }
    
    private fun showInvalidQrCodeMessage() {
        Toast.makeText(this, "Ungültiger QR-Code. Bitte scannen Sie einen gültigen CBRN-Trainer QR-Code.", Toast.LENGTH_LONG).show()
        finish()
    }
    
    // Hilfsmethode zum Sperren der Orientierung auf Portrait
    private fun IntentIntegrator.lockOrientation() {
        val captureActivity = this.captureActivity
        if (captureActivity == null) {
            this.captureActivity = PortraitCaptureActivity::class.java
        }
    }
} 