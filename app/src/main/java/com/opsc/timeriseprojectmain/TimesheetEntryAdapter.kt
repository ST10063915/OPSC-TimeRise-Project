package com.opsc.timeriseprojectmain

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class TimesheetEntryAdapter2(
    private var entries: List<TimesheetEntry>
) : RecyclerView.Adapter<TimesheetEntryAdapter2.EntryViewHolder>() {

    class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val descriptionTextView: TextView = view.findViewById(R.id.text_description)
        private val dateTextView: TextView = view.findViewById(R.id.text_date_time)
        private val imageViewEntry: ImageView = view.findViewById(R.id.image_entry)

        fun bind(entry: TimesheetEntry) {
            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            descriptionTextView.text = entry.description
            dateTextView.text = "${dateFormat.format(entry.date)} from ${entry.startTime} to ${entry.endTime}"

            entry.imageUrl?.let {
                if (it.startsWith("content://")) {
                    imageViewEntry.setImageURI(Uri.parse(it))
                } else {
                    Glide.with(imageViewEntry.context)
                        .load(it)
                        .into(imageViewEntry)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timesheet_entry2, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount() = entries.size

    // Update entries and notify dataset changed
    fun updateEntries(newEntries: List<TimesheetEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }
}
