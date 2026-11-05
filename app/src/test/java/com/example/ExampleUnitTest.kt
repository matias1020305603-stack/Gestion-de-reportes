package com.example

import com.example.util.UsernameValidator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testValidUsernames() {
    val valid1 = UsernameValidator.validate("carlos.mora")
    assertTrue(valid1.isValid)

    val valid2 = UsernameValidator.validate("prof_rodriguez")
    assertTrue(valid2.isValid)

    val valid3 = UsernameValidator.validate("est.santiago")
    assertTrue(valid3.isValid)
  }

  @Test
  fun testSensualUsernamesRejected() {
    val res1 = UsernameValidator.validate("sexy_santi")
    assertFalse(res1.isValid)
    assertTrue(res1.message?.contains("sensual") == true)

    val res2 = UsernameValidator.validate("s3xy_girl")
    assertFalse(res2.isValid)

    val res3 = UsernameValidator.validate("hot_boy_2026")
    assertFalse(res3.isValid)
  }

  @Test
  fun testRudeUsernamesRejected() {
    val res1 = UsernameValidator.validate("el_puto_amo")
    assertFalse(res1.isValid)
    assertTrue(res1.message?.contains("maleducadas") == true || res1.message?.contains("No permitido") == true)

    val res2 = UsernameValidator.validate("mierda_test")
    assertFalse(res2.isValid)

    val res3 = UsernameValidator.validate("pendejo_123")
    assertFalse(res3.isValid)
  }

  @Test
  fun testLengthConstraints() {
    val short = UsernameValidator.validate("ab")
    assertFalse(short.isValid)

    val long = UsernameValidator.validate("supercalifragilisticoespialidoso")
    assertFalse(long.isValid)
  }
}
