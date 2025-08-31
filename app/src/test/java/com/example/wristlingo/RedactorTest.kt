package com.example.wristlingo

import com.example.wristlingo.data.Redactor
import org.junit.Assert.assertEquals
import org.junit.Test

class RedactorTest {
  @Test fun redactEmail() {
    val s = "Contact me at user@example.com"
    val r = Redactor.redact(s)
    assertEquals("Contact me at [email]", r)
  }

  @Test fun redactPhone() {
    val s = "Call +1 415 555 1212 please"
    val r = Redactor.redact(s)
    assertEquals("Call [phone] please", r)
  }
}

