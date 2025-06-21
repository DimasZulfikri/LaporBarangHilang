package com.example.laporbaranghilang.kotlin.adapter // GANTI DENGAN PACKAGE NAME ANDA!

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.laporbaranghilang.R // Import R class untuk resource
import com.example.laporbaranghilang.kotlin.DetailReportActivity // Sesuaikan import
import com.example.laporbaranghilang.kotlin.model.Report // Sesuaikan import model
import java.text.SimpleDateFormat
import java.util.*

class ReportAdapter(private val context: Context, private val reportList: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportList[position]
        holder.itemNameTextView.text = report.itemName
        holder.descriptionTextView.text = report.description
        holder.locationTextView.text = "Lokasi: ${report.lastLocation}"
        holder.dateLostTextView.text = "Hilang: ${report.dateLost}"

        // Handle null timestamp gracefully
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val formattedDate = report.timestamp.let { timestamp ->
            if (timestamp != 0L) sdf.format(Date(timestamp)) else "N/A"
        }
        holder.timestampTextView.text = "Dilaporkan: $formattedDate"

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailReportActivity::class.java)
            intent.putExtra("report_id", report.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return reportList.size
    }

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemNameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        val dateLostTextView: TextView = itemView.findViewById(R.id.dateLostTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }
}