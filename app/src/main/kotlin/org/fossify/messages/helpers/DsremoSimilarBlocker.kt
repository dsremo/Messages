package org.fossify.messages.helpers

import android.content.Context

object DsremoSimilarBlocker {

    private const val PREFS = "dsremo_similar_blocker"
    private const val KEY_SIGNATURES = "signatures"
    private const val MAX_SIGNATURES = 200
    private const val SIGNATURE_TOKEN_COUNT = 5
    private const val MIN_TOKEN_LEN = 4

    private val nonAlphanumeric = Regex("[^a-z0-9]+")
    private val urlPattern = Regex("https?://\\S+|www\\.\\S+|[\\w-]+\\.(com|in|net|org|co|io|app|xyz|top|click|link)\\b")
    private val digitRun = Regex("\\d+")
    private val stopwords = setOf(
        "the", "and", "for", "you", "your", "with", "from", "this", "that", "have",
        "will", "are", "has", "was", "but", "not", "all", "can", "use", "now",
        "click", "here", "tap", "link", "url", "via", "any", "may", "get", "make",
    )

    fun signatureOf(body: String): String {
        if (body.isBlank()) return ""
        val cleaned = body.lowercase()
            .replace(urlPattern, " ")
            .replace(digitRun, " ")
            .replace(nonAlphanumeric, " ")
        val tokens = cleaned.split(" ")
            .filter { token -> token.length >= MIN_TOKEN_LEN && token !in stopwords }
        if (tokens.isEmpty()) return ""
        return tokens
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(SIGNATURE_TOKEN_COUNT)
            .joinToString("|") { it.key }
    }

    fun addSignature(context: Context, body: String) {
        val signature = signatureOf(body)
        if (signature.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_SIGNATURES, emptySet()) ?: emptySet()
        if (signature in current) return
        val updated = if (current.size >= MAX_SIGNATURES) {
            current.drop(current.size - MAX_SIGNATURES + 1).toMutableSet().also { it.add(signature) }
        } else {
            current.toMutableSet().also { it.add(signature) }
        }
        prefs.edit().putStringSet(KEY_SIGNATURES, updated).apply()
    }

    fun isSimilarToBlocked(context: Context, body: String): Boolean {
        val signature = signatureOf(body)
        if (signature.isBlank()) return false
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SIGNATURES, emptySet()) ?: return false
        if (signature in stored) return true
        val incomingTokens = signature.split("|").toSet()
        if (incomingTokens.size < 3) return false
        return stored.any { stored ->
            val storedTokens = stored.split("|").toSet()
            if (storedTokens.size < 3) return@any false
            val intersection = incomingTokens.intersect(storedTokens).size
            val union = incomingTokens.union(storedTokens).size
            union > 0 && intersection.toDouble() / union.toDouble() >= 0.6
        }
    }
}
