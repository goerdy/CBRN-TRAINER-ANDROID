package de.cbrntrainer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.view.ViewGroup

class OnboardingActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PENDING_ACTION = "pending_action"
        const val EXTRA_PENDING_DATA = "pending_data"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var skipButton: Button
    private lateinit var nextButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        skipButton = findViewById(R.id.skipButton)
        nextButton = findViewById(R.id.nextButton)
        
        setupViewPager()
        setupButtons()
    }
    
    private fun setupViewPager() {
        val adapter = OnboardingAdapter()
        viewPager.adapter = adapter

        // Dots-Indikator
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Tabs quadratisch machen, damit die Indikatoren rund bleiben
        tabLayout.post {
            for (i in 0 until tabLayout.tabCount) {
                val tab = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
                val params = tab.layoutParams
                params.width = dpToPx(24)
                params.height = dpToPx(24)
                tab.layoutParams = params
            }
        }

        // Button-Text basierend auf Position
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtonText(position)
            }
        })
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    private fun setupButtons() {
        skipButton.setOnClickListener {
            finishOnboarding()
        }
        
        nextButton.setOnClickListener {
            if (viewPager.currentItem == 6) { // Letzter Slide
                finishOnboarding()
            } else {
                viewPager.currentItem += 1
            }
        }
    }
    
    private fun updateButtonText(position: Int) {
        nextButton.text = if (position == 6) "Los geht's!" else "Weiter"
    }
    
    private fun finishOnboarding() {
        // Markiere Onboarding als abgeschlossen
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()

        val mainIntent = Intent(this, MainActivity::class.java)
        intent.getStringExtra(EXTRA_PENDING_ACTION)?.let { mainIntent.action = it }
        intent.getStringExtra(EXTRA_PENDING_DATA)?.let { mainIntent.data = Uri.parse(it) }

        // Starte MainActivity
        startActivity(mainIntent)
        finish()
    }
}
