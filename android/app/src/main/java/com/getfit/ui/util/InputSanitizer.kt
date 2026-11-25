package com.getfit.ui.util

/**
 * Input sanitization and validation helpers for user-entered auth/profile fields.
 */
object InputSanitizer {
    const val MAX_PASSWORD_LENGTH = 128
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_NAME_LENGTH = 60

    /**
     * Allows only printable ASCII characters (0x21..0x7E) in passwords:
     * strips spaces, tabs, newlines, control chars, zero-width/format chars, and emojis/non-ASCII.
     */
    fun sanitizePasswordInput(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length.coerceAtMost(MAX_PASSWORD_LENGTH))
        for (ch in input) {
            if (isAllowedPasswordChar(ch)) {
                sb.append(ch)
                if (sb.length >= MAX_PASSWORD_LENGTH) break
            }
        }
        return sb.toString()
    }

    fun isAllowedPasswordChar(ch: Char): Boolean = ch in '\u0021'..'\u007E'

    fun validatePassword(password: String, isSignUp: Boolean): String? {
        if (password.isEmpty()) return "Please enter your password."
        if (password.any { !isAllowedPasswordChar(it) }) {
            return "Password cannot contain spaces or unsupported characters."
        }
        if (isSignUp && password.length < MIN_PASSWORD_LENGTH) {
            return "Password must be at least $MIN_PASSWORD_LENGTH characters long."
        }
        if (password.length > MAX_PASSWORD_LENGTH) {
            return "Password must be at most $MAX_PASSWORD_LENGTH characters."
        }
        return null
    }

    /**
     * Allows Unicode letters, single spaces between words, hyphens, apostrophes, and periods.
     * Strips digits, emojis/surrogates, control/format chars, symbols (< > @ # $ etc.),
     * leading spaces, and consecutive spaces.
     */
    fun sanitizeNameInput(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length.coerceAtMost(MAX_NAME_LENGTH))
        var prevWasSpace = false
        for (ch in input) {
            when {
                ch == ' ' -> {
                    if (sb.isNotEmpty() && !prevWasSpace) {
                        sb.append(' ')
                        prevWasSpace = true
                    }
                }
                isAllowedNameChar(ch) -> {
                    if (sb.isEmpty() && (ch == '-' || ch == '.' || ch == '\'' || ch == '\u2019')) {
                        continue
                    }
                    sb.append(ch)
                    prevWasSpace = false
                }
            }
            if (sb.length >= MAX_NAME_LENGTH) break
        }
        return sb.toString()
    }

    fun isAllowedNameChar(ch: Char): Boolean {
        if (ch.isISOControl() || ch.isSurrogate()) return false
        if (Character.getType(ch) == Character.FORMAT.toInt()) return false
        return ch.isLetter() || ch == '-' || ch == '\'' || ch == '\u2019' || ch == '.'
    }

    fun validateName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Please enter your name."
        if (trimmed.none { it.isLetter() }) return "Please enter a valid name using letters."
        if (trimmed.any { it != ' ' && !isAllowedNameChar(it) }) {
            return "Name contains unsupported characters."
        }
        return null
    }
}
