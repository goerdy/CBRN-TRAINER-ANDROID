package com.example.myapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BeaconListAdapter(
    private val beacons: List<BeaconData>,
    private val onAddClick: (BeaconData) -> Unit
) : RecyclerView.Adapter<BeaconListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.deviceName)
        val deviceAddress: TextView = view.findViewById(R.id.deviceAddress)
        val rssiValue: TextView = view.findViewById(R.id.rssiValue)
        val addButton: Button = view.findViewById(R.id.addButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_beacon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val beacon = beacons[position]
        holder.deviceName.text = beacon.name ?: "Unbekanntes Gerät"
        holder.deviceAddress.text = beacon.address
        holder.rssiValue.text = "RSSI: ${beacon.rssi} dBm"
        
        holder.addButton.setOnClickListener {
            onAddClick(beacon)
        }
    }

    override fun getItemCount() = beacons.size
} 