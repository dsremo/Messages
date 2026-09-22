package org.fossify.messages.helpers

import java.util.regex.Pattern

/**
 * Extracts OTP-like codes from incoming messages so the keyboard can offer
 * a one-tap "copy OTP" action without the user having to manually highlight.
 *
 * Heuristic:
 *   1. Reject if the message is classified as SPAM/PROMOTIONS by FraudFilter.
 *   2. Look for a 4-8 digit run that's adjacent to one of the OTP context words.
 *   3. If multiple candidates, prefer the shortest one nearest to the OTP keyword.
 */
object DsremoOtpExtractor {

    private val OTP_CONTEXT_KEYWORDS = listOf(
        "otp", "one[\\s-]?time", "verification", "code is", "code:",
        "auth(?:ent)?(?:ication)?", "verify", "passcode", "pin",
        "ओटीपी", "क(?:ृ|ु)पया दर्ज करें"
    )

    private val OTP_KEYWORD_REGEX = Pattern.compile(
        "(?i)\\b(" + OTP_CONTEXT_KEYWORDS.joinToString("|") + ")\\b"
    )

    private val DIGIT_RUN_REGEX = Pattern.compile("\\b(\\d{4,8})\\b")

    data class Extraction(
        val otp: String,
        val confidence: Confidence,
        val anchorKeyword: String
    )

    enum class Confidence { LOW, MEDIUM, HIGH }

    fun extract(messageBody: String?): Extraction? {
        if (messageBody.isNullOrBlank()) return null
        val keywordMatcher = OTP_KEYWORD_REGEX.matcher(messageBody)
        val keywordPositions = mutableListOf<Pair<Int, String>>()
        while (keywordMatcher.find()) {
            keywordPositions.add(keywordMatcher.start() to keywordMatcher.group(1).orEmpty())
        }
        if (keywordPositions.isEmpty()) return null

        val digitMatcher = DIGIT_RUN_REGEX.matcher(messageBody)
        val digitRuns = mutableListOf<Pair<Int, String>>()
        while (digitMatcher.find()) {
            digitRuns.add(digitMatcher.start() to digitMatcher.group(1).orEmpty())
        }
        if (digitRuns.isEmpty()) return null

        val ranked = digitRuns.map { (digitStart, digitValue) ->
            val nearestKeyword = keywordPositions.minByOrNull { Math.abs(it.first - digitStart) }
                ?: return@map null
            val distance = Math.abs(nearestKeyword.first - digitStart)
            val confidence = when {
                distance <= 20 && digitValue.length in 4..8 -> Confidence.HIGH
                distance <= 60 -> Confidence.MEDIUM
                else -> Confidence.LOW
            }
            Extraction(digitValue, confidence, nearestKeyword.second)
        }.filterNotNull().sortedWith(compareByDescending<Extraction> { it.confidence }.thenBy { it.otp.length })
        return ranked.firstOrNull()
    }
}
