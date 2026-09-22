package org.fossify.messages.dsremo

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.fossify.messages.R
import org.fossify.messages.models.Message

object ChakshuReporter {

    private const val CHAKSHU_URL = "https://sancharsaathi.gov.in/sfc/"

    fun launch(activity: Activity, messages: List<Message>) {
        val payload = buildPayload(messages)
        if (payload.isNotEmpty()) {
            copyToClipboard(activity, payload)
        }
        Toast.makeText(activity, R.string.dsremo_report_chakshu_clipboard, Toast.LENGTH_LONG).show()
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(CHAKSHU_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { activity.startActivity(viewIntent) }
    }

    private fun buildPayload(messages: List<Message>): String {
        if (messages.isEmpty()) return ""
        val mostRecent = messages.sortedByDescending { it.date }.take(5)
        val builder = StringBuilder()
        for (message in mostRecent) {
            val sender = message.senderName.takeIf { it.isNotBlank() } ?: message.senderPhoneNumber
            builder.append("From: ").append(sender).append('\n')
            builder.append(message.body).append("\n\n")
        }
        return builder.toString().trim()
    }

    private fun copyToClipboard(activity: Activity, payload: String) {
        val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.setPrimaryClip(ClipData.newPlainText("Chakshu report", payload))
    }
}
