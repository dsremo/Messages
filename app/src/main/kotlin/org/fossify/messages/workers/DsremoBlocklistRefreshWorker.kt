package org.fossify.messages.workers

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.fossify.messages.helpers.DsremoUrlBlocklist
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class DsremoBlocklistRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val feedResults = coroutineScope {
                FEEDS.map { feedUrl ->
                    async(Dispatchers.IO) {
                        runCatching { downloadAndExtractHosts(feedUrl) }
                    }
                }.awaitAll()
            }
            val combinedHosts = HashSet<String>(800_000)
            var anyFailed = false
            for (feedResult in feedResults) {
                val hosts = feedResult.getOrNull()
                if (hosts == null) {
                    anyFailed = true
                    continue
                }
                combinedHosts.addAll(hosts)
            }
            if (combinedHosts.isEmpty()) {
                return Result.retry()
            }
            if (anyFailed && combinedHosts.size < 100_000) {
                return Result.retry()
            }
            val hashArray = LongArray(combinedHosts.size)
            var writeIndex = 0
            for (hostEntry in combinedHosts) {
                hashArray[writeIndex] = DsremoUrlBlocklist.fnv1a64(hostEntry)
                writeIndex++
            }
            java.util.Arrays.sort(hashArray)

            val filesDir = applicationContext.filesDir
            val tempFile = File(filesDir, "$FILE_NAME.new")
            val finalFile = File(filesDir, FILE_NAME)
            withContext(Dispatchers.IO) {
                DataOutputStream(tempFile.outputStream().buffered()).use { out ->
                    for (hashValue in hashArray) {
                        out.writeLong(hashValue)
                    }
                }
            }
            if (!tempFile.renameTo(finalFile)) {
                finalFile.delete()
                if (!tempFile.renameTo(finalFile)) {
                    return Result.retry()
                }
            }
            DsremoUrlBlocklist.invalidate()
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun downloadAndExtractHosts(feedUrl: String): List<String> {
        val hosts = ArrayList<String>()
        val connection = URL(feedUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.requestMethod = "GET"
        connection.doInput = true
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("HTTP $status for $feedUrl")
            }
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.lineSequence().forEach { rawLine ->
                    val host = parseLine(rawLine) ?: return@forEach
                    hosts.add(host)
                }
            }
        } finally {
            connection.disconnect()
        }
        return hosts
    }

    private fun parseLine(rawLine: String): String? {
        var line = rawLine.trim()
        if (line.isEmpty()) return null
        if (line.startsWith("#") || line.startsWith("!")) return null
        if (line.startsWith("||")) {
            val end = line.indexOf('^')
            line = if (end > 2) line.substring(2, end) else line.substring(2)
        }
        if (line.startsWith("0.0.0.0 ")) {
            line = line.substring("0.0.0.0 ".length).trim()
        } else if (line.startsWith("127.0.0.1 ")) {
            line = line.substring("127.0.0.1 ".length).trim()
        }
        val hashIndex = line.indexOf('#')
        if (hashIndex >= 0) {
            line = line.substring(0, hashIndex).trim()
        }
        if (line.startsWith("*.")) {
            line = line.substring(2)
        }
        if (line.isEmpty()) return null
        val extracted = if (line.contains("://")) {
            runCatching { Uri.parse(line).host }.getOrNull()
        } else {
            line
        } ?: return null
        val normalized = extracted.trim().lowercase().trim('.')
        if (normalized.isEmpty() || normalized.contains(' ')) return null
        if (!normalized.contains('.')) return null
        return normalized
    }

    companion object {
        private const val FILE_NAME = "dsremo_url_hashes.bin"
        private const val PERIODIC_NAME = "dsremo_blocklist_refresh"
        private const val ONE_TIME_NAME = "dsremo_blocklist_refresh_now"
        private val FEEDS = listOf(
            "https://urlhaus.abuse.ch/downloads/text/",
            "https://openphish.com/feed.txt",
            "https://threatfox.abuse.ch/downloads/hostfile/",
            "https://phishing.army/download/phishing_army_blocklist_extended.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/tif.mini-onlydomains.txt",
            "https://www.joewein.net/dl/bl/dom-bl-base.txt",
            "https://raw.githubusercontent.com/PolishFiltersTeam/KADhosts/master/KADomains.txt",
            "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-domains-ACTIVE.txt",
        )

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
            val request = PeriodicWorkRequestBuilder<DsremoBlocklistRefreshWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun refreshNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<DsremoBlocklistRefreshWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
