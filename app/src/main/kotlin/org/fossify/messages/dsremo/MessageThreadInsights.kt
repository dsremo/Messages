package org.fossify.messages.dsremo

/**
 * Per-thread analytics for an SMS conversation.
 *
 * Surfaces "you exchange ~40 messages a week with this contact, mostly
 * Mon-Fri after 6pm, average response time 12 minutes" — quick relationship
 * snapshot that helps the user spot stale conversations or unread ones
 * they should pick up.
 */
object MessageThreadInsights {

    data class TimedMessage(
        val incoming: Boolean,
        val timestampMs: Long,
        val charCount: Int
    )

    data class ThreadStats(
        val totalMessages: Int,
        val incomingCount: Int,
        val outgoingCount: Int,
        val avgResponseTimeMinIncoming: Int,
        val avgResponseTimeMinOutgoing: Int,
        val mostActiveHourBucket: String,
        val charsExchanged: Int,
        val lastActivityMs: Long
    )

    fun compute(messages: List<TimedMessage>): ThreadStats {
        if (messages.isEmpty()) {
            return ThreadStats(0, 0, 0, 0, 0, "—", 0, 0)
        }
        val sorted = messages.sortedBy { it.timestampMs }
        val incomingCount = sorted.count { it.incoming }
        val outgoingCount = sorted.count { !it.incoming }
        val (avgInResp, avgOutResp) = computeResponseTimes(sorted)
        val hourBuckets = mutableMapOf<Int, Int>()
        for (msg in sorted) {
            val hour = java.util.Calendar.getInstance().apply { timeInMillis = msg.timestampMs }
                .get(java.util.Calendar.HOUR_OF_DAY)
            val bucket = hour - (hour % 3)
            hourBuckets[bucket] = (hourBuckets[bucket] ?: 0) + 1
        }
        val topHour = hourBuckets.maxByOrNull { it.value }?.key ?: 0
        val bucketLabel = "${topHour.pad()}–${(topHour + 3).pad()}"
        val charsExchanged = sorted.sumOf { it.charCount }
        return ThreadStats(
            totalMessages = sorted.size,
            incomingCount = incomingCount,
            outgoingCount = outgoingCount,
            avgResponseTimeMinIncoming = avgInResp,
            avgResponseTimeMinOutgoing = avgOutResp,
            mostActiveHourBucket = bucketLabel,
            charsExchanged = charsExchanged,
            lastActivityMs = sorted.last().timestampMs
        )
    }

    private fun computeResponseTimes(sortedMessages: List<TimedMessage>): Pair<Int, Int> {
        var incomingRespSum = 0L
        var incomingRespCount = 0
        var outgoingRespSum = 0L
        var outgoingRespCount = 0
        for (idx in 1 until sortedMessages.size) {
            val previous = sortedMessages[idx - 1]
            val current = sortedMessages[idx]
            if (previous.incoming == current.incoming) continue
            val gapMin = (current.timestampMs - previous.timestampMs) / 60_000L
            if (gapMin > 24 * 60) continue
            if (current.incoming) {
                incomingRespSum += gapMin
                incomingRespCount++
            } else {
                outgoingRespSum += gapMin
                outgoingRespCount++
            }
        }
        val avgIn = if (incomingRespCount > 0) (incomingRespSum / incomingRespCount).toInt() else 0
        val avgOut = if (outgoingRespCount > 0) (outgoingRespSum / outgoingRespCount).toInt() else 0
        return avgIn to avgOut
    }

    private fun Int.pad(): String = if (this < 10) "0$this" else this.toString()
}
