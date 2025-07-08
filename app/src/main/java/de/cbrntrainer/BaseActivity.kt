package de.cbrntrainer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

open class BaseActivity : AppCompatActivity() {
    
    // Flag, um zu verfolgen, ob wir im Vollbildmodus sein sollen
    protected var useFullscreen = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
    
    // Methode zum Anzeigen der System-UI
    protected fun showSystemUI() {
        // Neue Methode für Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, true)
        // Setze die Farbe der Navigationsleiste und Statusleiste
        window.navigationBarColor = ContextCompat.getColor(this, R.color.main_background)
        window.statusBarColor = ContextCompat.getColor(this, R.color.main_background)
    }
    
    // Methode zum Ausblenden der System-UI (für Messgeräte)
    protected fun hideSystemUI() {
        useFullscreen = true
        enableFullscreen()
    }
} 