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
import org.fossify.messages.helpers.DsremoUrlBlocklist
import java.io.BufferedReader
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
            val hosts = sortedSetOf<String>()
            for (feedUrl in FEEDS) {
                for (host in downloadAndExtractHosts(feedUrl)) {
                    hosts.add(host)
                }
            }
            if (hosts.isEmpty()) {
                return Result.retry()
            }
            val filesDir = applicationContext.filesDir
            val tempFile = File(filesDir, "$FILE_NAME.new")
            val finalFile = File(filesDir, FILE_NAME)
            tempFile.bufferedWriter().use { writer ->
                for (host in hosts) {
                    writer.write(host)
                    writer.newLine()
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
                    val line = rawLine.trim()
                    if (line.isEmpty()) return@forEach
                    if (line.startsWith("#")) return@forEach
                    val normalizedUrl = if (line.contains("://")) line else "http://$line"
                    val parsedHost = runCatching { Uri.parse(normalizedUrl).host }.getOrNull()
                    val host = parsedHost?.lowercase()?.trimStart('.')
                    if (!host.isNullOrBlank()) {
                        hosts.add(host)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return hosts
    }

    companion object {
        private const val FILE_NAME = "dsremo_url_hosts.txt"
        private const val PERIODIC_NAME = "dsremo_blocklist_refresh"
        private const val ONE_TIME_NAME = "dsremo_blocklist_refresh_now"
        private val FEEDS = listOf(
            "https://urlhaus.abuse.ch/downloads/text/",
            "https://openphish.com/feed.txt",
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
