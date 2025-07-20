package de.cbrntrainer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

open class BaseActivity : AppCompatActivity() {
    
    // Flag, um zu verfolgen, ob wir im Vollbildmodus sein sollen
    protected var useFullscreen = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Display immer an lassen (wie bei Medienwiedergabe)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Standardmäßig Systemelemente anzeigen (kein Vollbild)
        showSystemUI()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Nur Vollbildmodus wiederherstellen, wenn das Flag gesetzt ist
            if (useFullscreen) {
                enableFullscreen()
            } else {
                showSystemUI()
            }
        }
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        // Nur Vollbildmodus wiederherstellen, wenn das Flag gesetzt ist
        if (useFullscreen) {
            enableFullscreen()
        }
    }
    
    private fun enableFullscreen() {
        // Neue Methode für Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
    
    // Methode zum Anzeigen der System-UI (Statusbar und Navigation Bar ausgeblendet)
    protected fun showSystemUI() {
        // Vollbildmodus - Statusbar und Navigation Bar ausblenden
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Statusbar und Navigation Bar ausblenden
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        
        // Vollbildmodus aktivieren
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    
    // Methode zum Ausblenden der System-UI (für Messgeräte)
    protected fun hideSystemUI() {
        useFullscreen = true
        enableFullscreen()
    }
} 