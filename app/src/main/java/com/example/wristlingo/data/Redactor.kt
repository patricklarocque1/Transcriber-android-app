package com.example.wristlingo.data

object Redactor {
  private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
  private val phone = Regex("\\b(\\+?\\d[\\d -]{7,}\\d)\\b")
  fun redact(s: String): String = s
    .replace(email, "[email]")
    .replace(phone, "[phone]")
}

