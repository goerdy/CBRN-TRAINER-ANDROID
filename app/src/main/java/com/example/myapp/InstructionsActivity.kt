package com.example.myapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        // Zurück-Button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish() // Schließt diese Activity und kehrt zur vorherigen zurück
        }
    }
} 