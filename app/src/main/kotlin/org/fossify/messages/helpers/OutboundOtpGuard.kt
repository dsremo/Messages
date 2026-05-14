package org.fossify.messages.helpers

import org.fossify.messages.models.Message
import java.util.regex.Pattern

object OutboundOtpGuard {

    private const val RECENT_RECEIVE_WINDOW_MS = 15L * 60L * 1000L

    private val P_OTP_DIGIT_RUN = Pattern.compile("\\b\\d{4,8}\\b")
    private val P_BODY_IS_OTP = Pattern.compile(
        "^\\s*\\d{4,8}\\s*$"
    )
    private val P_BODY_HAS_OTP_AND_DIGITS = Pattern.compile(
        "(?i)\\b(otp|code|pin|ओटीपी)\\b.*?\\b\\d{4,8}\\b|\\b\\d{4,8}\\b.{0,15}\\b(otp|code|pin|ओटीपी)\\b"
    )
    private val P_RECEIVED_OTP_CONTEXT = Pattern.compile(
        "(?i)\\b(OTP|One[\\s-]?Time[\\s-]?Password|verification[\\s-]?code|verify|verification|ओटीपी|do not share|don'?t share)\\b"
    )

    fun outboundTextLooksLikeOtpShare(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > 200) return false
        if (P_BODY_IS_OTP.matcher(trimmed).matches()) return true
        if (P_BODY_HAS_OTP_AND_DIGITS.matcher(trimmed).find()) return true
        return false
    }

    fun threadRecentlyReceivedOtp(
        messages: List<Message>,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val cutoffSecs = ((nowMillis - RECENT_RECEIVE_WINDOW_MS) / 1000L).toInt()
        for (message in messages) {
            val isReceived = message.type == android.provider.Telephony.Sms.MESSAGE_TYPE_INBOX
            if (!isReceived) continue
            if (message.date < cutoffSecs) continue
            if (P_RECEIVED_OTP_CONTEXT.matcher(message.body).find() &&
                P_OTP_DIGIT_RUN.matcher(message.body).find()
            ) {
                return true
            }
        }
        return false
    }

    fun shouldGuard(outboundText: String, recentMessages: List<Message>): Boolean {
        if (!outboundTextLooksLikeOtpShare(outboundText)) return false
        return threadRecentlyReceivedOtp(recentMessages)
    }
}
