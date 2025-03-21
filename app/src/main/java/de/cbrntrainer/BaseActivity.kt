package de.cbrntrainer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

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
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
    
    // Methode zum Anzeigen der System-UI
    protected fun showSystemUI() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
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