package com.opsc.timeriseprojectmain

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class MyListAdapter(private val localDataSet: Array<String>) :
    RecyclerView.Adapter<MyListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.item_text)
        val button: Button = view.findViewById(R.id.item_button)
        val imageView: ImageView = view.findViewById(R.id.item_image)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.textView.text = localDataSet[position]
        viewHolder.imageView.setImageResource(R.drawable.screenshot_2024_04_28_191523) // Placeholder for item image
        viewHolder.button.setOnClickListener {
            Toast.makeText(it.context, "Edit ${localDataSet[position]}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = localDataSet.size
}