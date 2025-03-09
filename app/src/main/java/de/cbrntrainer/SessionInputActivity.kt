package de.cbrntrainer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SessionInputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_input)

        val sessionIdInput = findViewById<EditText>(R.id.sessionIdInput)
        
        // Verbinden-Button
        findViewById<Button>(R.id.connectButton).setOnClickListener {
            val sessionId = sessionIdInput.text.toString().trim()
            
            if (sessionId.length == 4) {
                // Starte WebView mit der Session-ID
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra("SESSION_ID", sessionId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Bitte geben Sie eine gültige 4-stellige Session-ID ein", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Zurück-Button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
} 