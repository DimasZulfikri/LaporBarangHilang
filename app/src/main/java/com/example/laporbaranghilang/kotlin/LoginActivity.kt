package com.example.laporbaranghilang.kotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase // Import FirebaseDatabase
import com.example.laporbaranghilang.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase // Deklarasi database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance() // Inisialisasi database

        // Jika user sudah login, langsung arahkan ke MainActivity dan cek role
        auth.currentUser?.let { user ->
            checkUserRoleAndNavigate(user.uid)
            return
        }

        binding.loginButton.setOnClickListener { loginUser() }
        binding.registerButton.setOnClickListener { registerUser() }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan password wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                    task.result.user?.uid?.let { uid ->
                        checkUserRoleAndNavigate(uid)
                    }
                } else {
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan password wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            binding.passwordEditText.error = "Password minimal 6 karakter!"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    val uid = task.result.user?.uid
                    uid?.let {
                        // Simpan role default "user" ke database
                        val userMap = hashMapOf(
                            "email" to email,
                            "role" to "user" // Default role untuk user baru
                        )
                        database.getReference("users").child(it).setValue(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                                navigateToMainActivity(Role.USER) // Navigasi sebagai user
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan info user: ${e.message}", Toast.LENGTH_LONG).show()
                                // Meskipun gagal menyimpan role, tetap navigasi (bisa ditangani lebih baik)
                                navigateToMainActivity(Role.USER)
                            }
                    }
                } else {
                    Toast.makeText(this, "Registrasi gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        database.getReference("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val roleString = snapshot.child("role").getValue(String::class.java)
                val role = Role.fromString(roleString)
                navigateToMainActivity(role)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mendapatkan peran pengguna.", Toast.LENGTH_SHORT).show()
                // Fallback ke user biasa jika gagal mendapatkan peran
                navigateToMainActivity(Role.USER)
            }
    }

    private fun navigateToMainActivity(role: Role) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_role", role.name) // Kirim peran ke MainActivity
        startActivity(intent)
        finish()
    }
}