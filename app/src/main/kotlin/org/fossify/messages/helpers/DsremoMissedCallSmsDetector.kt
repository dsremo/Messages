package org.fossify.messages.helpers

object DsremoMissedCallSmsDetector {

    private val missedCallPattern = Regex(
        "(?i)\\b(missed[\\s_-]*call|miss[\\s_-]*ed[\\s_-]*call|call[\\s_-]*missed|" +
            "you[\\s_-]+missed[\\s_-]+a[\\s_-]+call|missed[\\s_-]+call[\\s_-]+alert|" +
            "missed[\\s_-]+call[\\s_-]+from|missedcall|callmissed)\\b"
    )

    private val callLogSummaryPattern = Regex(
        "(?i)\\b(call[\\s_-]+summary|missed[\\s_-]+calls[\\s_-]+today|" +
            "voicemail[\\s_-]+alert|new[\\s_-]+voicemail|" +
            "you[\\s_-]+have[\\s_-]+\\d+[\\s_-]+missed[\\s_-]+calls?)\\b"
    )

    private val operatorMissedCallSenders = setOf(
        "MissedCall", "MISSCAL", "MISSEDCALL", "MOTAJM", "VOICEMSG",
        "VK-MISSED", "AX-MISSED", "JK-MISSED", "BP-MISSED",
    )

    fun isMissedCallSms(address: String, body: String): Boolean {
        if (body.isBlank()) return false
        if (missedCallPattern.containsMatchIn(body)) return true
        if (callLogSummaryPattern.containsMatchIn(body)) return true
        val senderUpper = address.uppercase()
        if (operatorMissedCallSenders.any { senderUpper.contains(it) }) {
            if (missedCallPattern.containsMatchIn(body.lowercase()) ||
                body.lowercase().contains("call")) return true
        }
        return false
    }
}
