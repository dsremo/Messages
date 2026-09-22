package org.fossify.messages.helpers

import android.content.Context
import java.io.DataInputStream
import java.io.File
import java.util.Arrays

object DsremoUrlBlocklist {

    private const val ASSET_NAME = "dsremo_url_hashes.bin"
    private const val CACHE_NAME = "dsremo_url_hashes.bin"
    private const val FNV_OFFSET_BASIS: Long = -3750763034362895579L
    private const val FNV_PRIME: Long = 1099511628211L

    @Volatile
    private var loadedHashes: LongArray? = null

    @Volatile
    private var loadedSource: String = "bundled"

    fun load(context: Context): LongArray {
        val cached = loadedHashes
        if (cached != null) return cached
        return synchronized(this) {
            val existing = loadedHashes
            if (existing != null) return@synchronized existing
            val (data, source) = readFrom(context)
            loadedHashes = data
            loadedSource = source
            data
        }
    }

    fun contains(context: Context, host: String): Boolean {
        val normalized = normalize(host) ?: return false
        val hashes = load(context)
        val target = fnv1a64(normalized)
        return Arrays.binarySearch(hashes, target) >= 0
    }

    fun sourceName(context: Context): String {
        load(context)
        return loadedSource
    }

    fun invalidate() {
        synchronized(this) { loadedHashes = null }
    }

    fun fnv1a64(input: String): Long {
        var hash = FNV_OFFSET_BASIS
        for (byteIndex in input.indices) {
            val byteValue = input[byteIndex].code and 0xFF
            hash = hash xor byteValue.toLong()
            hash *= FNV_PRIME
        }
        return hash
    }

    private fun normalize(host: String): String? {
        val trimmed = host.trim().lowercase().trim('.')
        if (trimmed.isEmpty() || trimmed.contains(' ')) return null
        return trimmed
    }

    private fun readFrom(context: Context): Pair<LongArray, String> {
        val cacheFile = File(context.filesDir, CACHE_NAME)
        if (cacheFile.exists() && cacheFile.length() > 0 && cacheFile.length() % 8L == 0L) {
            runCatching { return readLongs(cacheFile.inputStream(), (cacheFile.length() / 8L).toInt()) to "cache" }
        }
        val assetStream = context.assets.open(ASSET_NAME)
        val length = context.assets.openFd(ASSET_NAME).length
        val entries = (length / 8L).toInt()
        return readLongs(assetStream, entries) to "bundled"
    }

    private fun readLongs(input: java.io.InputStream, entries: Int): LongArray {
        DataInputStream(input.buffered()).use { stream ->
            val out = LongArray(entries)
            for (writeIndex in 0 until entries) {
                out[writeIndex] = stream.readLong()
            }
            return out
        }
    }
}
