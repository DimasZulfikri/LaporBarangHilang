package com.example.laporbaranghilang.kotlin

enum class Role {
    USER,
    HOST;

    companion object {
        fun fromString(roleString: String?): Role {
            return when (roleString?.lowercase()) {
                "host" -> HOST
                else -> USER // Default ke USER jika tidak ditemukan atau null
            }
        }
    }
}