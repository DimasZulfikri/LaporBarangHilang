package com.example.laporbaranghilang.kotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.laporbaranghilang.databinding.ActivityDetailReportBinding
import com.example.laporbaranghilang.kotlin.model.Report
import java.text.SimpleDateFormat
import java.util.*

class DetailReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailReportBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var reportId: String? = null
    private var currentReport: Report? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().getReference("reports")
        auth = FirebaseAuth.getInstance()

        reportId = intent.getStringExtra("report_id")
        if (reportId == null) {
            Toast.makeText(this, "ID Laporan tidak ditemukan!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadReportDetails()

        binding.editButton.setOnClickListener { toggleEditMode(true) }
        binding.deleteButton.setOnClickListener { confirmDelete() }
        binding.saveChangesButton.setOnClickListener { updateReport() }
        binding.cancelButton.setOnClickListener { toggleEditMode(false) }

        // --- FITUR BARU: LISTENER UNTUK TOMBOL STATUS/QR ---
        binding.btnChangeStatus.setOnClickListener { confirmChangeStatus() }
        binding.btnShowQr.setOnClickListener { showQrCode() }
    }

    private fun loadReportDetails() {
        reportId?.let {
            database.child(it).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentReport = snapshot.getValue(Report::class.java)
                    currentReport?.let { report ->
                        // Tampilkan data di TextViews
                        binding.detailItemName.text = report.itemName
                        binding.detailDescription.text = report.description
                        binding.detailLastLocation.text = report.lastLocation
                        binding.detailDateLost.text = report.dateLost

                        // Format timestamp
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val formattedDate = report.timestamp.let { timestamp -> sdf.format(Date(timestamp)) }
                        binding.detailTimestamp.text = "Dilaporkan: $formattedDate"

                        // Set nilai awal untuk EditTexts di mode edit
                        binding.editItemName.setText(report.itemName)
                        binding.editDescription.setText(report.description)
                        binding.editLastLocation.setText(report.lastLocation)
                        binding.editDateLost.setText(report.dateLost)

                        // --- FITUR BARU: Tampilkan Status dan Atur Visibilitas Tombol ---
                        binding.detailStatus.text = report.status

                        // Hanya pemilik laporan yang bisa mengedit/menghapus dan mengubah status/menampilkan QR
                        if (auth.currentUser?.uid == report.userId) {
                            binding.editButton.visibility = View.VISIBLE
                            binding.deleteButton.visibility = View.VISIBLE

                            when (report.status) {
                                "Hilang" -> {
                                    binding.btnChangeStatus.visibility = View.VISIBLE
                                    binding.btnChangeStatus.text = "Tandai Barang Ditemukan"
                                    binding.btnShowQr.visibility = View.GONE
                                }
                                "Ditemukan" -> {
                                    binding.btnChangeStatus.visibility = View.VISIBLE
                                    binding.btnChangeStatus.text = "Barang Sudah Diambil" // Tombol untuk ganti status ke "Sudah Diambil"
                                    binding.btnShowQr.visibility = View.VISIBLE // Tampilkan tombol QR jika sudah ditemukan
                                }
                                "Sudah Diambil" -> {
                                    binding.btnChangeStatus.visibility = View.GONE
                                    binding.btnShowQr.visibility = View.GONE // QR tidak perlu lagi ditampilkan jika sudah diambil
                                    binding.detailStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                                }
                            }
                        } else {
                            binding.editButton.visibility = View.GONE
                            binding.deleteButton.visibility = View.GONE
                            binding.btnChangeStatus.visibility = View.GONE
                            binding.btnShowQr.visibility = View.GONE
                        }

                    } ?: run {
                        Toast.makeText(this@DetailReportActivity, "Laporan tidak ditemukan.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DetailReportActivity, "Gagal memuat detail laporan: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun toggleEditMode(editMode: Boolean) {
        if (editMode) {
            binding.displayModeLayout.visibility = View.GONE
            binding.editModeLayout.visibility = View.VISIBLE
        } else {
            binding.displayModeLayout.visibility = View.VISIBLE
            binding.editModeLayout.visibility = View.GONE
        }
    }

    private fun updateReport() {
        val newName = binding.editItemName.text.toString().trim()
        val newDescription = binding.editDescription.text.toString().trim()
        val newLocation = binding.editLastLocation.text.toString().trim()
        val newDate = binding.editDateLost.text.toString().trim()

        if (newName.isEmpty() || newDescription.isEmpty() || newLocation.isEmpty() || newDate.isEmpty()) {
            Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        currentReport?.let { report ->
            report.itemName = newName
            report.description = newDescription
            report.lastLocation = newLocation
            report.dateLost = newDate

            reportId?.let { id ->
                database.child(id).setValue(report)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Laporan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            toggleEditMode(false)
                        } else {
                            Toast.makeText(this, "Gagal memperbarui laporan: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Laporan")
            .setMessage("Apakah Anda yakin ingin menghapus laporan ini?")
            .setPositiveButton("Hapus") { dialog, which ->
                deleteReport()
            }
            .setNegativeButton("Batal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteReport() {
        reportId?.let { id ->
            database.child(id).removeValue()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Laporan berhasil dihapus!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal menghapus laporan: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    // --- FITUR BARU: LOGIKA UNTUK STATUS DAN QR ---
    private fun confirmChangeStatus() {
        val newStatus = when (currentReport?.status) {
            "Hilang" -> "Ditemukan"
            "Ditemukan" -> "Sudah Diambil"
            else -> return // Tidak melakukan apa-apa jika status sudah "Sudah Diambil"
        }

        AlertDialog.Builder(this)
            .setTitle("Ubah Status Barang")
            .setMessage("Apakah Anda yakin ingin mengubah status barang menjadi '$newStatus'?")
            .setPositiveButton("Ya") { dialog, which ->
                updateReportStatus(newStatus)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun updateReportStatus(newStatus: String) {
        reportId?.let { id ->
            val updates = HashMap<String, Any>()
            updates["status"] = newStatus

            // Jika status menjadi "Ditemukan", buat atau update data QR Code
            if (newStatus == "Ditemukan" && currentReport?.qrCodeData == null) {
                // Gunakan ID laporan sebagai data QR Code
                updates["qrCodeData"] = id
            }

            database.child(id).updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Status berhasil diperbarui menjadi '$newStatus'", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal memperbarui status: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun showQrCode() {
        currentReport?.let { report ->
            report.qrCodeData?.let { qrData ->
                val intent = Intent(this, QrViewerActivity::class.java)
                intent.putExtra("qr_data", qrData)
                intent.putExtra("item_name", report.itemName) // Kirim nama barang juga
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Data QR Code belum tersedia untuk laporan ini.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}