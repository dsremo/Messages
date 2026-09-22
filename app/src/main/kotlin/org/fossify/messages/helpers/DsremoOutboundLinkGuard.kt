package org.fossify.messages.helpers

import java.util.regex.Pattern

object DsremoOutboundLinkGuard {

    private val P_URL = Pattern.compile(
        "(?i)\\b(?:https?://|www\\.)[^\\s]+\\b"
    )

    data class Warning(
        val matchedUrl: String,
        val reasons: List<String>,
    )

    fun analyze(text: String): Warning? {
        val matcher = P_URL.matcher(text)
        if (!matcher.find()) return null
        val raw = matcher.group()
        val analysis = DsremoUrlSanitizer.analyze(raw)
        if (analysis.warnings.isEmpty() && !analysis.refuse) return null
        val reasons = analysis.warnings.toMutableList()
        if (analysis.refuse) {
            reasons.add(0, "Refusing executable-file URL (.apk/.exe/...)")
        }
        return Warning(matchedUrl = analysis.sanitized, reasons = reasons)
    }
}
