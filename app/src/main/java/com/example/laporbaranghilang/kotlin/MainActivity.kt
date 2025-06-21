package com.example.laporbaranghilang.kotlin

import android.content.Intent
import android.os.Bundle
import android.util.Log // Import untuk debugging dengan Logcat
import android.view.Menu
import android.view.MenuItem
import android.view.View // Import untuk mengatur visibilitas View (misal TextView)
import android.widget.TextView // Import untuk referensi TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.example.laporbaranghilang.R // Pastikan ini diimpor untuk mengakses R.drawable atau R.id
import com.example.laporbaranghilang.databinding.ActivityMainBinding
import com.example.laporbaranghilang.kotlin.adapter.ReportAdapter
import com.example.laporbaranghilang.kotlin.model.Report
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var reportAdapter: ReportAdapter
    private val reportList = mutableListOf<Report>()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference // Referensi ke node 'reports'
    private lateinit var userRoleRef: DatabaseReference // Referensi ke node 'users/{uid}'
    private var currentUserRole: Role = Role.USER // Role pengguna saat ini, defaultnya USER

    // Tag untuk Logcat, memudahkan filtering log
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("reports")

        // Inisialisasi userRoleRef dengan UID pengguna yang sedang login
        // Jika UID null (misal belum login), arahkan kembali ke LoginActivity
        auth.currentUser?.uid?.let { uid ->
            userRoleRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        } ?: run {
            Toast.makeText(this, "Sesi pengguna berakhir. Silakan login kembali.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Tutup MainActivity agar user tidak bisa kembali ke sini tanpa login
            return // Keluar dari onCreate
        }

        setupRecyclerView()
        // Panggil fungsi untuk memeriksa dan memuat peran pengguna,
        // yang kemudian akan memuat daftar laporan
        checkAndLoadUserRole()

        // Listener untuk Floating Action Button (FAB)
        binding.fabAddReport.setOnClickListener {
            if (currentUserRole == Role.USER) {
                // Jika user adalah USER, FAB akan membuka AddReportActivity
                startActivity(Intent(this, AddReportActivity::class.java))
            } else if (currentUserRole == Role.HOST) {
                // Jika user adalah HOST, FAB akan membuka HostScannerActivity
                startActivity(Intent(this, HostScannerActivity::class.java))
            } else {
                // Kondisi lain (misal role tidak dikenal, meskipun seharusnya tidak terjadi)
                Toast.makeText(this, "Fungsi ini tidak tersedia untuk peran Anda.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Memuat peran pengguna dari Firebase Realtime Database
     * dan kemudian memanggil updateUIForRole() serta loadReports().
     */
    private fun checkAndLoadUserRole() {
        userRoleRef.child("role").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Ambil nilai role sebagai String dari snapshot
                val roleString = snapshot.getValue(String::class.java)
                // Konversi String role ke enum Role
                currentUserRole = Role.fromString(roleString)

                // --- DEBUGGING: Tampilkan peran pengguna melalui Toast dan Logcat ---
                Toast.makeText(this@MainActivity, "Anda login sebagai: ${currentUserRole.name}", Toast.LENGTH_LONG).show()
                Log.d(TAG, "User role: ${currentUserRole.name} for UID: ${auth.currentUser?.uid}")

                // Sesuaikan tampilan antarmuka pengguna berdasarkan peran yang terdeteksi
                updateUIForRole()
                // Muat laporan setelah peran pengguna berhasil dikonfirmasi
                loadReports()
            }

            override fun onCancelled(error: DatabaseError) {
                // Tangani kesalahan jika gagal memuat peran pengguna
                Log.e(TAG, "Gagal memuat peran pengguna: ${error.message}", error.toException())
                Toast.makeText(this@MainActivity, "Gagal memuat peran pengguna: ${error.message}", Toast.LENGTH_LONG).show()
                // Jika terjadi kesalahan, anggap sebagai USER dan tetap coba muat laporan
                currentUserRole = Role.USER
                updateUIForRole()
                loadReports()
            }
        })
    }

    /**
     * Memperbarui elemen UI seperti ikon FAB dan pesan sambutan
     * berdasarkan peran pengguna saat ini.
     */
    private fun updateUIForRole() {
        if (currentUserRole == Role.HOST) {
            // Jika HOST, ganti ikon FAB ke ikon scan QR
            binding.fabAddReport.setImageResource(R.drawable.ic_qr_scan)
            binding.fabAddReport.contentDescription = "Scan QR Code" // Deskripsi aksesibilitas
        } else {
            // Jika USER, gunakan ikon default untuk menambah (misal tanda plus)
            // Menggunakan android.R.drawable untuk ikon bawaan sistem Android
            binding.fabAddReport.setImageResource(android.R.drawable.ic_input_add)
            binding.fabAddReport.contentDescription = "Tambah Laporan Baru" // Deskripsi aksesibilitas
        }

        // Siapkan pesan sambutan sesuai dengan peran pengguna
        val welcomeMessage = if (currentUserRole == Role.HOST) {
            "Halo Host, siap memverifikasi barang?"
        } else {
            "Halo ${auth.currentUser?.email ?: "Pengguna"}, selamat datang!"
        }

        // Tampilkan pesan sambutan di welcomeTextView (jika ada di layout)
        // Pastikan Anda sudah menambahkan TextView dengan id "welcomeTextView" di activity_main.xml
        findViewById<TextView>(R.id.welcomeTextView)?.apply {
            text = welcomeMessage
            visibility = View.VISIBLE // Pastikan TextView terlihat
        }
    }

    /**
     * Mengatur RecyclerView dengan adapter dan layout manager.
     */
    private fun setupRecyclerView() {
        reportAdapter = ReportAdapter(this, reportList)
        binding.recyclerViewReports.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = reportAdapter
        }
    }

    /**
     * Memuat daftar laporan dari Firebase Realtime Database
     * berdasarkan peran pengguna.
     */
    private fun loadReports() {
        val query: Query = if (currentUserRole == Role.HOST) {
            // Jika HOST, muat semua laporan diurutkan berdasarkan timestamp
            database.orderByChild("timestamp")
        } else {
            // Jika USER, hanya muat laporan yang dibuat oleh user tersebut
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                database.orderByChild("userId").equalTo(currentUserId)
            } else {
                // Kasus ini seharusnya sudah ditangani di onCreate()
                // Namun, sebagai fallback, kita bisa log error dan kembali ke login
                Log.e(TAG, "UID pengguna tidak ditemukan saat memuat laporan. Kembali ke Login.")
                Toast.makeText(this, "Terjadi kesalahan. Silakan login kembali.", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return // Keluar dari fungsi
            }
        }

        // Menambahkan listener untuk mendapatkan pembaruan data secara real-time
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reportList.clear() // Hapus data lama
                for (postSnapshot in snapshot.children) {
                    val report = postSnapshot.getValue(Report::class.java)
                    report?.let { reportList.add(it) } // Tambahkan laporan baru
                }
                reportList.sortByDescending { it.timestamp } // Urutkan laporan dari yang terbaru
                reportAdapter.notifyDataSetChanged() // Beri tahu adapter bahwa data telah berubah
                Log.d(TAG, "Laporan berhasil dimuat. Jumlah: ${reportList.size}. Role: ${currentUserRole.name}")
            }

            override fun onCancelled(error: DatabaseError) {
                // Tangani kesalahan jika pembacaan data dibatalkan
                Toast.makeText(this@MainActivity, "Gagal memuat data laporan: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Gagal memuat data laporan: ${error.message}", error.toException())
            }
        })
    }

    /**
     * Mengatur menu opsi di ActionBar.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu) // Inflate layout menu Anda
        return true
    }

    /**
     * Menangani pemilihan item dari menu opsi.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                // Logout dari Firebase Auth
                auth.signOut()
                // Arahkan kembali ke LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
                finish() // Tutup MainActivity
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}