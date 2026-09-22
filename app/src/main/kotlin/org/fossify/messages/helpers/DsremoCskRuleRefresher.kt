package org.fossify.messages.helpers

import android.content.Context
import org.fossify.commons.helpers.ensureBackgroundThread
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

/**
 * Light-weight CERT-In CSK alerts refresher. Runs on every app launch but
 * only actually fetches when last refresh is > 7 days old; otherwise no-op.
 * Extracts known malware family names from the CSK alerts page and stores
 * them in SharedPreferences for FraudFilter to consult.
 *
 * No WorkManager dependency — App.onCreate triggers the check on a
 * background thread; the network call is fail-soft.
 */
object DsremoCskRuleRefresher {

    private const val PREFS = "dsremo_csk_rules"
    private const val KEY_LAST_REFRESH = "last_refreshed_ts"
    private const val KEY_KEYWORDS = "extracted_keywords"
    private const val REFRESH_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L
    private const val CSK_INDEX_URL = "https://www.csk.gov.in/alerts/"

    private val P_MALWARE_NAME = Pattern.compile(
        "(?i)\\b(SpyMax|SpyNote|Cerberus|Anubis|TeaBot|Octo|Hydra|BRATA|FakeCalls|" +
            "Ginp|MoqHao|SharkBot|FluBot|Joker|Xenomorph|SoumniBot|Coyote|Drinik|Daam)\\b"
    )

    fun scheduleOpportunistic(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_REFRESH, 0L)
        if (System.currentTimeMillis() - last < REFRESH_INTERVAL_MS) return
        ensureBackgroundThread {
            runCatching { fetchAndStore(context) }
        }
    }

    private fun fetchAndStore(context: Context) {
        val html = runCatching {
            val conn = (URL(CSK_INDEX_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "dsremo-messages/csk-refresher")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { reader -> reader.readText() }
                } else null
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return

        val found = mutableSetOf<String>()
        val matcher = P_MALWARE_NAME.matcher(html)
        while (matcher.find()) found.add(matcher.group())

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_REFRESH, System.currentTimeMillis())
            .putString(KEY_KEYWORDS, JSONArray(found.toList()).toString())
            .apply()
    }

    fun getKnownMalwareNames(context: Context): List<String> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_KEYWORDS, null) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { idx -> arr.getString(idx) }
    }.getOrDefault(emptyList())

    fun getLastRefresh(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_REFRESH, 0L)
}
