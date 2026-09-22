package org.fossify.messages.helpers

import android.content.Context

object BlockedMessageStore {
    private const val PREFS_NAME = "dsremo_blocked_urls"

    data class Entry(val host: String, val source: String, val timestamp: Long)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, messageId: Long, entry: Entry) {
        val serialized = "${entry.host}|${entry.source}|${entry.timestamp}"
        prefs(context).edit().putString(messageId.toString(), serialized).apply()
    }

    fun get(context: Context, messageId: Long): Entry? {
        val serialized = prefs(context).getString(messageId.toString(), null) ?: return null
        return parse(serialized)
    }

    fun clear(context: Context, messageId: Long) {
        prefs(context).edit().remove(messageId.toString()).apply()
    }

    fun all(context: Context): Map<Long, Entry> {
        val result = HashMap<Long, Entry>()
        for ((key, value) in prefs(context).all) {
            val messageId = key.toLongOrNull() ?: continue
            val serialized = value as? String ?: continue
            val entry = parse(serialized) ?: continue
            result[messageId] = entry
        }
        return result
    }

    private fun parse(serialized: String): Entry? {
        val parts = serialized.split("|")
        if (parts.size != 3) return null
        val timestamp = parts[2].toLongOrNull() ?: return null
        return Entry(host = parts[0], source = parts[1], timestamp = timestamp)
    }
}
