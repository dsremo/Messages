package org.fossify.messages.dsremo

/**
 * Quick validity check on India PIN codes that appear in delivery SMS.
 *
 * Catches "fake delivery" scam SMS that include a non-existent PIN
 * (e.g. "your parcel ABCD123 is held at 999999") — real India PINs
 * are 6 digits and the first digit must be 1-8.
 */
object IndianPostalCodeValidator {

    fun isValidIndiaPin(candidate: String?): Boolean {
        if (candidate == null) return false
        if (candidate.length != 6) return false
        if (!candidate.all { it.isDigit() }) return false
        val firstDigit = candidate[0].digitToInt()
        return firstDigit in 1..8
    }

    fun regionForPin(pin: String): String? {
        if (!isValidIndiaPin(pin)) return null
        return when (pin[0]) {
            '1' -> "Delhi / Haryana / Punjab / Himachal / J&K"
            '2' -> "Uttar Pradesh / Uttarakhand"
            '3' -> "Rajasthan / Gujarat / Daman & Diu"
            '4' -> "Maharashtra / Madhya Pradesh / Chhattisgarh / Goa"
            '5' -> "Andhra Pradesh / Telangana / Karnataka"
            '6' -> "Tamil Nadu / Puducherry / Kerala / Lakshadweep"
            '7' -> "West Bengal / Odisha / Northeast"
            '8' -> "Bihar / Jharkhand"
            else -> null
        }
    }

    fun extractPinFromMessage(messageBody: String?): String? {
        if (messageBody.isNullOrBlank()) return null
        val regex = Regex("\\b([1-8]\\d{5})\\b")
        return regex.find(messageBody)?.value
    }
}
