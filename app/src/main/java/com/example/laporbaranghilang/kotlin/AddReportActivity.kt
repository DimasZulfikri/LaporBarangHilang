package com.example.laporbaranghilang.kotlin // GANTI DENGAN PACKAGE NAME ANDA!

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.laporbaranghilang.databinding.ActivityAddReportBinding // Import View Binding
import com.example.laporbaranghilang.kotlin.model.Report // Sesuaikan import model

class AddReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddReportBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().getReference("reports")
        auth = FirebaseAuth.getInstance()

        binding.saveButton.setOnClickListener { saveReport() }
    }

    private fun saveReport() {
        val itemName = binding.itemNameEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val lastLocation = binding.lastLocationEditText.text.toString().trim()
        val dateLost = binding.dateLostEditText.text.toString().trim()
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Pengguna belum login.", Toast.LENGTH_SHORT).show()
            return
        }

        if (itemName.isEmpty() || description.isEmpty() || lastLocation.isEmpty() || dateLost.isEmpty()) {
            Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val reportId = database.push().key
        val timestamp = System.currentTimeMillis()

        if (reportId != null) {
            val report = Report(reportId, userId, itemName, description, lastLocation, dateLost, timestamp)
            database.child(reportId).setValue(report)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Laporan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal menambahkan laporan: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(this, "Gagal membuat ID laporan.", Toast.LENGTH_SHORT).show()
        }
    }
}