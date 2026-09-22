package org.fossify.messages.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import org.fossify.messages.receivers.DeleteSmsReceiver
import java.util.regex.Pattern

object DsremoOtpDetector {
    private val P_OTP_KEYWORDS = Pattern.compile(
        "(?i)\\b(otp|code|verification|verify|password|passcode|one[- ]?time|2fa|two[- ]?factor)\\b"
    )
    private val P_SHORT_DIGIT_RUN = Pattern.compile("\\b\\d{4,8}\\b")
    private const val MAX_BODY_LENGTH = 2000

    fun looksLikeOtp(body: String?): Boolean {
        if (body.isNullOrBlank()) return false
        if (body.length > MAX_BODY_LENGTH) return false
        val hasKeyword = P_OTP_KEYWORDS.matcher(body).find()
        val hasDigits = P_SHORT_DIGIT_RUN.matcher(body).find()
        return hasKeyword && hasDigits
    }

    fun scheduleDeletion(context: Context, threadId: Long, messageId: Long, delayMinutes: Int) {
        val intent = Intent(context, DeleteSmsReceiver::class.java).apply {
            putExtra(THREAD_ID, threadId)
            putExtra(MESSAGE_ID, messageId)
            putExtra(IS_MMS, false)
        }
        val requestCode = (messageId and 0x7fffffffL).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = SystemClock.elapsedRealtime() + delayMinutes * 60_000L
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
    }
}
