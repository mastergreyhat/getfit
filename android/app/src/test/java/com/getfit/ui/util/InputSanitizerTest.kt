package com.getfit.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InputSanitizerTest {

    @Test
    fun password_stripsSpacesControlCharsAndEmojis() {
        assertEquals("Pass123!@#", InputSanitizer.sanitizePasswordInput(" Pass 123\t\n!@#\u200B😀 "))
    }

    @Test
    fun password_validationChecksLengthAndDisallowedChars() {
        assertNotNull(InputSanitizer.validatePassword("", isSignUp = true))
        assertNotNull(InputSanitizer.validatePassword("abc12", isSignUp = true))
        assertNotNull(InputSanitizer.validatePassword("abc 123", isSignUp = false))
        assertNull(InputSanitizer.validatePassword("Str0ng!Pass", isSignUp = true))
    }

    @Test
    fun name_stripsDigitsSymbolsEmojisAndCollapsesSpaces() {
        assertEquals("Mary-Jane O'Connor", InputSanitizer.sanitizeNameInput("  Mary-Jane   O'Connor123!@#😀"))
        assertEquals("Dr. José ", InputSanitizer.sanitizeNameInput("Dr. José "))
        assertEquals("scriptAlex", InputSanitizer.sanitizeNameInput("-123<script>Alex"))
    }

    @Test
    fun name_validationRequiresLetters() {
        assertNotNull(InputSanitizer.validateName("   "))
        assertNotNull(InputSanitizer.validateName("---"))
        assertNull(InputSanitizer.validateName("Aswin Vijay"))
    }
}
