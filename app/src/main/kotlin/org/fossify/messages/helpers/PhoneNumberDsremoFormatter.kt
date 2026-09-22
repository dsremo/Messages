package org.fossify.messages.helpers

object PhoneNumberDsremoFormatter {

    fun format(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val trimmedNumber = rawNumber.trim()
        val hasPlus = trimmedNumber.startsWith("+")
        val digitsOnly = trimmedNumber.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return trimmedNumber

        return when {
            hasPlus && digitsOnly.startsWith("91") && digitsOnly.length == 12 ->
                "+91 ${digitsOnly.substring(2, 7)} ${digitsOnly.substring(7)}"
            !hasPlus && digitsOnly.length == 10 ->
                "${digitsOnly.substring(0, 5)} ${digitsOnly.substring(5)}"
            !hasPlus && digitsOnly.startsWith("91") && digitsOnly.length == 12 ->
                "+91 ${digitsOnly.substring(2, 7)} ${digitsOnly.substring(7)}"
            hasPlus && digitsOnly.length in 10..15 ->
                "+${digitsOnly.chunked(4).joinToString(" ")}"
            else -> trimmedNumber
        }
    }
}
