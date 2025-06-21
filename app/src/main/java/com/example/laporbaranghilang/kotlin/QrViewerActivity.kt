package com.example.laporbaranghilang.kotlin

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.example.laporbaranghilang.databinding.ActivityQrViewerBinding

class QrViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val qrData = intent.getStringExtra("qr_data")
        val itemName = intent.getStringExtra("item_name")

        if (qrData.isNullOrEmpty()) {
            Toast.makeText(this, "Data QR tidak valid.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.qrCodeDataTextView.text = "ID Laporan: $itemName ($qrData)" // Tampilkan data laporan di bawah QR
        generateQrCode(qrData)
    }

    private fun generateQrCode(data: String) {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            binding.qrCodeImageView.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal membuat QR Code: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}