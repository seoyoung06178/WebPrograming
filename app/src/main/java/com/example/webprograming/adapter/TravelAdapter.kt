package com.example.webprograming.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.webprograming.R
import com.example.webprograming.model.TravelRecord
import java.io.File

class TravelAdapter(
    private var records: MutableList<TravelRecord>,
    private val onItemClick: (TravelRecord) -> Unit,
    private val onItemLongClick: (TravelRecord, View) -> Unit
) : RecyclerView.Adapter<TravelAdapter.TravelViewHolder>() {

    class TravelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_travel, parent, false)
        return TravelViewHolder(view)
    }

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        val record = records[position]

        holder.tvTitle.text = record.title
        holder.tvDate.text = record.visitDate

        if (record.photoPath.isNotEmpty() && File(record.photoPath).exists()) {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = BitmapFactory.decodeFile(record.photoPath, options)
            holder.ivThumbnail.setImageBitmap(bitmap)
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_placeholder)
        }

        holder.itemView.setOnClickListener { onItemClick(record) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(record, it)
            true
        }
    }

    override fun getItemCount(): Int = records.size

    fun updateRecords(newRecords: List<TravelRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }

    fun getRecordAt(position: Int): TravelRecord = records[position]
}
