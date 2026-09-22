package org.fossify.messages.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.style.URLSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.fossify.messages.R

class DsremoSafeUrlSpan(
    private val urlInSpan: String,
) : URLSpan(urlInSpan) {

    override fun onClick(widget: View) {
        val context = widget.context
        val analysis = DsremoUrlSanitizer.analyze(urlInSpan)

        if (analysis.refuse) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.dsremo_safe_url_refused),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        if (analysis.warnings.isEmpty()) {
            openExternally(context, Uri.parse(analysis.sanitized))
            return
        }
        showConfirmDialog(context, analysis)
    }

    private fun showConfirmDialog(
        context: Context,
        analysis: DsremoUrlSanitizer.Analysis,
    ) {
        val message = StringBuilder().apply {
            append(context.getString(R.string.dsremo_safe_url_about_to_open))
            append("\n\n")
            append(analysis.sanitized)
            if (analysis.warnings.isNotEmpty()) {
                append("\n\n")
                append(context.getString(R.string.dsremo_safe_url_warnings))
                append("\n• ")
                append(analysis.warnings.joinToString("\n• "))
            }
        }.toString()
        AlertDialog.Builder(context)
            .setTitle(R.string.dsremo_safe_url_title)
            .setMessage(message)
            .setNeutralButton(R.string.dsremo_safe_url_copy) { _, _ ->
                copyToClipboard(context, analysis.sanitized)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.dsremo_safe_url_open) { _, _ ->
                openExternally(context, Uri.parse(analysis.sanitized))
            }
            .show()
    }

    private fun openExternally(context: Context, uri: Uri) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun copyToClipboard(context: Context, url: String) {
        runCatching {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("url", url))
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.dsremo_safe_url_copied),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
