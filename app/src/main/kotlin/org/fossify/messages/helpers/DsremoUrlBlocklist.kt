package org.fossify.messages.helpers

import android.content.Context
import java.io.File

object DsremoUrlBlocklist {
    private const val FILE_NAME = "dsremo_url_hosts.txt"

    @Volatile
    private var cached: Set<String>? = null

    fun load(context: Context): Set<String> {
        val existing = cached
        if (existing != null) return existing
        synchronized(this) {
            val stillExisting = cached
            if (stillExisting != null) return stillExisting
            val hosts = HashSet<String>(20000)
            val cacheFile = File(context.filesDir, FILE_NAME)
            val reader = if (cacheFile.exists()) {
                cacheFile.bufferedReader()
            } else {
                context.assets.open(FILE_NAME).bufferedReader()
            }
            reader.use { bufferedReader ->
                bufferedReader.lineSequence().forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.isEmpty()) return@forEach
                    if (line.startsWith("#")) return@forEach
                    hosts.add(line.lowercase())
                }
            }
            cached = hosts
            return hosts
        }
    }

    fun contains(context: Context, host: String): Boolean {
        if (host.isBlank()) return false
        return load(context).contains(host.lowercase().trimStart('.'))
    }

    fun sourceName(context: Context): String {
        val cacheFile = File(context.filesDir, FILE_NAME)
        return if (cacheFile.exists()) "cache" else "bundled"
    }

    fun invalidate() {
        synchronized(this) {
            cached = null
        }
    }
}
