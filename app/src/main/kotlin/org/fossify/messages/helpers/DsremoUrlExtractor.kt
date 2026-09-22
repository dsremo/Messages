package org.fossify.messages.helpers

import android.net.Uri
import android.util.Patterns

object DsremoUrlExtractor {
    fun extractHosts(body: String): List<String> {
        if (body.isBlank()) return emptyList()
        val hosts = LinkedHashSet<String>()
        runCatching {
            val matcher = Patterns.WEB_URL.matcher(body)
            while (matcher.find()) {
                val rawUrl = matcher.group() ?: continue
                val normalizedUrl = if (rawUrl.contains("://")) rawUrl else "http://$rawUrl"
                val parsedHost = runCatching { Uri.parse(normalizedUrl).host }.getOrNull()
                val host = parsedHost?.lowercase()?.trimStart('.')
                if (!host.isNullOrBlank()) {
                    hosts.add(host)
                }
            }
        }
        return hosts.toList()
    }
}
