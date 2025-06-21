package com.example.laporbaranghilang.kotlin.model

data class Report(
    var id: String? = null,
    var userId: String? = null, // ID user yang melaporkan
    var itemName: String? = null,
    var description: String? = null,
    var lastLocation: String? = null,
    var dateLost: String? = null,
    var timestamp: Long = 0L,
    var status: String = "Hilang", // Status: "Hilang", "Ditemukan", "Sudah Diambil"
    var qrCodeData: String? = null // Data yang akan di-encode ke QR Code (akan sama dengan id laporan)
)