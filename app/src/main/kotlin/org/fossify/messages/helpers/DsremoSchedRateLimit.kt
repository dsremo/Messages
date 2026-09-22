package org.fossify.messages.helpers

import android.content.Context
import org.fossify.messages.extensions.messagesDB

object DsremoSchedRateLimit {

    fun countNearbyScheduledForAddresses(
        context: Context,
        targetAddresses: Collection<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (targetAddresses.isEmpty()) return 0
        val normalizedTargets = targetAddresses
            .mapNotNull { normalize(it) }
            .toSet()
        if (normalizedTargets.isEmpty()) return 0

        val windowStart = nowMillis - DSREMO_SCHED_RATE_LIMIT_WINDOW_MS
        val windowEnd = nowMillis + DSREMO_SCHED_RATE_LIMIT_WINDOW_MS

        val scheduled = try {
            context.messagesDB.getAllScheduledMessages()
        } catch (_: Exception) {
            return 0
        }

        return scheduled.count { scheduledMessage ->
            val sendAt = scheduledMessage.millis()
            if (sendAt < windowStart || sendAt > windowEnd) return@count false
            val messageAddresses = scheduledMessage.participants
                .flatMap { participant -> participant.phoneNumbers }
                .mapNotNull { phoneNumber -> normalize(phoneNumber.normalizedNumber) }
            messageAddresses.any { addr -> addr in normalizedTargets }
        }
    }

    private fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val digitsOnly = raw.filter { ch -> ch.isDigit() }
        if (digitsOnly.isEmpty()) return null
        return digitsOnly.takeLast(10)
    }
}
