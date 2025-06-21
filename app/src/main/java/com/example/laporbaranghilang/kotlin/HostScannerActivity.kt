package com.example.laporbaranghilang.kotlin

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.example.laporbaranghilang.databinding.ActivityHostScannerBinding
import com.example.laporbaranghilang.kotlin.model.Report

class HostScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostScannerBinding
    private val database = FirebaseDatabase.getInstance()
    private var currentScannedReportId: String? = null

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }

    // Register the launcher for the barcode scanner
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "QR Code Terdeteksi: " + result.contents, Toast.LENGTH_LONG).show()
            verifyReport(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScanQr.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        binding.btnVerifyCode.setOnClickListener {
            val uniqueCode = binding.uniqueCodeEditText.text.toString().trim()
            if (uniqueCode.isNotEmpty()) {
                verifyReport(uniqueCode)
            } else {
                Toast.makeText(this, "Silakan masukkan kode unik", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMarkAsTaken.setOnClickListener {
            currentScannedReportId?.let { id ->
                markReportAsTaken(id)
            } ?: Toast.makeText(this, "Tidak ada laporan yang diverifikasi untuk ditandai.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScan()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                Toast.makeText(this, "Izin kamera dibutuhkan untuk memindai QR.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startScan() {
        val options = ScanOptions()
        options.setPrompt("Arahkan kamera ke QR Code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        barcodeLauncher.launch(options)
    }

    private fun verifyReport(qrData: String) {
        database.getReference("reports").orderByChild("qrCodeData").equalTo(qrData)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (reportSnapshot in snapshot.children) {
                            val report = reportSnapshot.getValue(Report::class.java)
                            report?.let {
                                if (it.status == "Ditemukan") { // Hanya yang berstatus "Ditemukan" yang bisa diambil
                                    currentScannedReportId = it.id
                                    binding.verifiedReportDetails.visibility = View.VISIBLE
                                    binding.verifiedItemName.text = "Nama Barang: ${it.itemName}"
                                    binding.verifiedDescription.text = "Deskripsi: ${it.description}"
                                    binding.verifiedStatus.text = "Status: ${it.status}"
                                    binding.btnMarkAsTaken.visibility = View.VISIBLE
                                    Toast.makeText(this@HostScannerActivity, "Laporan ditemukan dan siap diambil!", Toast.LENGTH_LONG).show()
                                    return
                                } else if (it.status == "Hilang") {
                                    Toast.makeText(this@HostScannerActivity, "Barang masih berstatus 'Hilang'. Tidak bisa ditandai diambil.", Toast.LENGTH_LONG).show()
                                    clearVerifiedReportDetails()
                                    return
                                } else if (it.status == "Sudah Diambil") {
                                    Toast.makeText(this@HostScannerActivity, "Barang ini sudah diambil sebelumnya.", Toast.LENGTH_LONG).show()
                                    clearVerifiedReportDetails()
                                    return
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@HostScannerActivity, "Kode unik tidak ditemukan atau tidak valid.", Toast.LENGTH_LONG).show()
                        clearVerifiedReportDetails()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HostScannerActivity, "Gagal memverifikasi laporan: ${error.message}", Toast.LENGTH_LONG).show()
                    clearVerifiedReportDetails()
                }
            })
    }

    private fun markReportAsTaken(reportId: String) {
        val updates = HashMap<String, Any>()
        updates["status"] = "Sudah Diambil"

        database.getReference("reports").child(reportId).updateChildren(updates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Barang berhasil ditandai 'Sudah Diambil'!", Toast.LENGTH_SHORT).show()
                    clearVerifiedReportDetails()
                } else {
                    Toast.makeText(this, "Gagal menandai barang: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun clearVerifiedReportDetails() {
        binding.verifiedReportDetails.visibility = View.GONE
        currentScannedReportId = null
        binding.uniqueCodeEditText.text.clear()
    }
}