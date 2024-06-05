package com.opsc.timeriseprojectmain

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class TimesheetAdapter(private var timesheetEntries: List<TimesheetEntry>) :
    RecyclerView.Adapter<TimesheetAdapter.TimesheetViewHolder>() {

    class TimesheetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.text_date)
        val startTimeTextView: TextView = view.findViewById(R.id.text_start_time)
        val endTimeTextView: TextView = view.findViewById(R.id.text_end_time)
        val descriptionTextView: TextView = view.findViewById(R.id.text_description)
        val entryPhotoImageView: ImageView = view.findViewById(R.id.image_entry)

        fun bind(entry: TimesheetEntry) {
            dateTextView.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(entry.date)
            startTimeTextView.text = entry.startTime
            endTimeTextView.text = entry.endTime
            descriptionTextView.text = entry.description
            if (entry.imageUrl != null) {
                entryPhotoImageView.setImageURI(Uri.parse(entry.imageUrl))
            } else {
                entryPhotoImageView.setImageResource(R.drawable.ic_placeholder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimesheetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timesheet_entry, parent, false)
        return TimesheetViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimesheetViewHolder, position: Int) {
        holder.bind(timesheetEntries[position])
    }

    override fun getItemCount(): Int = timesheetEntries.size

    fun updateEntries(newEntries: List<TimesheetEntry>) {
        timesheetEntries = newEntries
        notifyDataSetChanged()
    }
}
