package de.cbrntrainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout

class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
    
    private val slides = listOf(
        OnboardingSlide(
            "CBRN-TRAINER",
            "Das digitale Hilfsmittel für innovative und qualitativ hochwertige Ausbildung für ABC/CBRN-Einsatz.",
            R.drawable.cbrn_trainer_logo
        ),
        OnboardingSlide(
            "Wichtiger Hinweis",
            "Diese App kann nicht die praktische Ausbildung an echten Geräten ersetzen. Sie eignet sich jedoch hervorragend für Taktikschulungen und Einsatzübungen.",
            R.drawable.realmessgeraet
        ),
        OnboardingSlide(
            "Cloud-Modus",
            "Für A-Messgeräte wenn keine Bluetoothbeacons zur Verfügung stehen sowie\nFür Gas-Messgeräte. Auch ohne App nutzbar!",
            R.drawable.cloudversion
        ),
        OnboardingSlide(
            "Bluetooth-Modus",
            "Benötigt Bluetooth Low Energy Beacons.\n\nRealistische Simulation mit physischen Beacons.\nEinstellbare Strahlungsquellen-Aktivität.",
            R.drawable.cbrn_trainer_logo
        ),
        OnboardingSlide(
            "Kontaminationsnachweis",
            "Benötigt Gerätekompass* + Neodym-Magnete\n\nSimulation von Kontaminationsnachweisgeräten\nNeodym-Magnete sind deutlich besser geeignet\n\n*Nicht in jedem Smartphone verbaut!",
            R.drawable.cbrn_trainer_logo
        ),
        OnboardingSlide(
            "Support & Hilfe",
            "Fehler bitte melden an:\ninfo@cbrn-trainer.de\n\nWeitere Informationen:\ncbrn-trainer.de\n\nBitte sieh von negativen Play Store Bewertungen ab.",
            R.drawable.ic_info
        ),
        OnboardingSlide(
            "Los geht's!",
            "Sie sind bereit für Ihre erste Übung!\n\nWählen Sie einen Modus und starten Sie mit der Ausbildung.",
            R.drawable.cbrn_trainer_logo
        )
    )
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_slide, parent, false)
        return OnboardingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(slides[position])
    }
    
    override fun getItemCount(): Int = slides.size
    
    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        
        fun bind(slide: OnboardingSlide) {
            titleText.text = slide.title
            descriptionText.text = slide.description
            imageView.setImageResource(slide.imageResId)
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val imageResId: Int
) 