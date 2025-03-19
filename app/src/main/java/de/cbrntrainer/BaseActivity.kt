package de.cbrntrainer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ActionBar ausblenden
        supportActionBar?.hide()
        
        // Vollbild-Modus (Immersive Mode) für alle Activities
        enableFullscreen()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Wenn die Activity den Fokus zurückerhält, Vollbildmodus wiederherstellen
            enableFullscreen()
        }
    }
    
    override fun onUserInteraction() {
        super.onUserInteraction()
        // Nach jeder Benutzerinteraktion den Vollbildmodus wiederherstellen
        enableFullscreen()
    }
    
    private fun enableFullscreen() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
} 