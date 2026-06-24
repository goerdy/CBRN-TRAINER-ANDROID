package de.cbrntrainer

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Bluetooth-Einstellungen Button
        findViewById<Button>(R.id.bluetoothSettingsButton).setOnClickListener {
            val intent = Intent(this, BluetoothSettingsActivity::class.java)
            startActivity(intent)
        }

        // Cloud-Einstellungen Button
        findViewById<Button>(R.id.cloudSettingsButton).setOnClickListener {
            startActivity(Intent(this, CloudSettingsActivity::class.java))
        }

        // Onboarding wiederholen Button
        findViewById<Button>(R.id.onboardingButton).setOnClickListener {
            // Lösche das Onboarding-Flag
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_completed", false)
                .apply()

            // Starte Onboarding
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
}