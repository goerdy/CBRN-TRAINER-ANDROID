package de.cbrntrainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SavedBeaconAdapter(
    private val beacons: List<BeaconData>,
    private val onRemoveClick: (BeaconData) -> Unit,
    private val onSettingsClick: (BeaconData) -> Unit,
    private val onItemClick: (BeaconData) -> Unit
) : RecyclerView.Adapter<SavedBeaconAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val deviceAddress: TextView = view.findViewById(R.id.deviceAddress)
        val rssiValue: TextView = view.findViewById(R.id.rssiValue)
        val settingsButton: ImageButton = view.findViewById(R.id.settingsButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_beacon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val beacon = beacons[position]
        holder.deviceName.text = beacon.name ?: "Unbekanntes Gerät"
        holder.deviceAddress.text = beacon.address
        holder.rssiValue.text = "RSSI: ${beacon.rssi} dBm"
        
        holder.itemView.setOnClickListener { onItemClick(beacon) }
        holder.settingsButton.setOnClickListener { onSettingsClick(beacon) }
        holder.deleteButton.setOnClickListener { onRemoveClick(beacon) }
    }

    override fun getItemCount() = beacons.size
} 