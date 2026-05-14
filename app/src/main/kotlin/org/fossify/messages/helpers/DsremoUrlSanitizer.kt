package org.fossify.messages.helpers

import android.net.Uri

object DsremoUrlSanitizer {

    private val SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.ly", "cutt.ly", "rebrand.ly", "is.gd",
        "qrco.de", "wa.me", "short.gy", "surl.li", "tiny.cc", "ow.ly",
        "buff.ly", "t.co", "rb.gy", "shorturl.at", "lnkd.in", "goo.gl",
    )
    private val SUSPICIOUS_TLDS = setOf(
        "top", "xyz", "buzz", "sbs", "click", "gq", "tk", "ml", "cf",
        "loan", "win", "date", "review", "country", "stream", "download",
        "party", "cricket", "trade", "men",
    )
    private val EXECUTABLE_EXT_RE = Regex(
        "\\.(apk|xapk|apks|exe|dmg|pkg|msi|bat|cmd|scr)\\b",
        RegexOption.IGNORE_CASE
    )
    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "fbclid", "gclid", "msclkid", "_ga", "mc_eid", "mc_cid",
        "igshid", "igsh", "ref", "referrer", "spm", "scm",
    )

    data class Analysis(
        val original: String,
        val sanitized: String,
        val warnings: List<String>,
        val refuse: Boolean,
    )

    fun analyze(rawUrl: String): Analysis {
        val warnings = mutableListOf<String>()
        var refuse = false
        val parsed = runCatching { Uri.parse(rawUrl) }.getOrNull()
            ?: return Analysis(rawUrl, rawUrl, emptyList(), false)

        val host = parsed.host?.lowercase().orEmpty()
        val path = parsed.path?.lowercase().orEmpty()
        val tld = host.substringAfterLast('.', missingDelimiterValue = "")

        if (host in SHORTENERS) {
            warnings.add("URL shortener — destination hidden until opened")
        }
        if (tld in SUSPICIOUS_TLDS) {
            warnings.add("Suspicious top-level domain: .$tld")
        }
        if (EXECUTABLE_EXT_RE.containsMatchIn(path)) {
            warnings.add("Executable file download")
            refuse = true
        }

        val cleaned = stripTrackingParams(parsed)
        val sanitizedStr = cleaned.toString()
        if (sanitizedStr != rawUrl) {
            warnings.add("Stripped tracking parameters")
        }
        return Analysis(rawUrl, sanitizedStr, warnings, refuse)
    }

    private fun stripTrackingParams(uri: Uri): Uri {
        val queryNames = runCatching { uri.queryParameterNames }.getOrDefault(emptySet())
        if (queryNames.isEmpty()) return uri
        val keep = queryNames.filter { paramName ->
            paramName.lowercase() !in TRACKING_PARAMS
        }
        if (keep.size == queryNames.size) return uri
        val builder = uri.buildUpon().clearQuery()
        for (paramName in keep) {
            uri.getQueryParameters(paramName).forEach { paramValue ->
                builder.appendQueryParameter(paramName, paramValue)
            }
        }
        return builder.build()
    }
}
