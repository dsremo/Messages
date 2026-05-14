package org.fossify.messages.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Telephony
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.helpers.FraudFilter
import org.fossify.messages.helpers.FraudVerdictStore
import org.fossify.messages.helpers.THREAD_ID
import org.fossify.messages.helpers.THREAD_TITLE

class DsremoFilteredActivity : SimpleActivity() {

    private data class Entry(
        val messageId: Long,
        val threadId: Long,
        val address: String,
        val body: String,
        val date: Long,
        val verdict: FraudFilter.Verdict,
    )

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getProperBackgroundColor())
            setPadding(24, 24, 24, 24)
        }
        val scroll = ScrollView(this).apply { addView(container) }

        val toolbar = androidx.appcompat.widget.Toolbar(this).apply {
            setBackgroundColor(getProperBackgroundColor())
            title = getString(R.string.dsremo_show_filtered)
            setTitleTextColor(getProperTextColor())
            setNavigationIcon(org.fossify.commons.R.drawable.ic_arrow_left_vector)
            setNavigationOnClickListener { finish() }
            navigationIcon?.setTint(getProperTextColor())
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getProperBackgroundColor())
            addView(
                toolbar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0, 1f
                )
            )
        }
        setContentView(rootLayout)

        title = getString(R.string.dsremo_show_filtered)
        loadAndRender()
    }

    private fun loadAndRender() {
        val placeholder = TextView(this).apply {
            text = getString(R.string.dsremo_filtered_loading)
            setTextColor(getProperTextColor())
            textSize = 14f
            setPadding(0, 24, 0, 24)
        }
        container.addView(placeholder)

        ensureBackgroundThread {
            val entries = collectEntries()
            runOnUiThread {
                container.removeAllViews()
                if (entries.isEmpty()) {
                    val empty = TextView(this).apply {
                        text = getString(R.string.dsremo_filtered_empty)
                        setTextColor(getProperTextColor())
                        textSize = 15f
                        gravity = Gravity.CENTER
                        setPadding(0, 96, 0, 0)
                    }
                    container.addView(empty)
                    return@runOnUiThread
                }
                addSection(FraudFilter.Category.SPAM, entries)
                addSection(FraudFilter.Category.PROMOTIONS, entries)
                addSection(FraudFilter.Category.INBOX, entries)
            }
        }
    }

    private fun collectEntries(): List<Entry> {
        val storeIds = runCatching {
            getSharedPreferences("dsremo_fraud_verdicts", MODE_PRIVATE).all.keys.toList()
        }.getOrDefault(emptyList())
        if (storeIds.isEmpty()) return emptyList()
        val entries = mutableListOf<Entry>()
        for (idStr in storeIds) {
            val messageId = idStr.toLongOrNull() ?: continue
            val verdict = FraudVerdictStore.get(this, messageId) ?: continue
            val cursor = runCatching {
                contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE,
                        Telephony.Sms.THREAD_ID,
                    ),
                    "${Telephony.Sms._ID} = ?",
                    arrayOf(messageId.toString()),
                    null
                )
            }.getOrNull() ?: continue
            cursor.use { messageCursor ->
                if (messageCursor.moveToFirst()) {
                    entries.add(
                        Entry(
                            messageId = messageId,
                            threadId = messageCursor.getLong(3),
                            address = messageCursor.getString(0).orEmpty(),
                            body = messageCursor.getString(1).orEmpty(),
                            date = messageCursor.getLong(2),
                            verdict = verdict,
                        )
                    )
                }
            }
        }
        return entries.sortedByDescending { entryItem -> entryItem.date }
    }

    private fun addSection(category: FraudFilter.Category, all: List<Entry>) {
        val filtered = all.filter { entryItem -> entryItem.verdict.category == category }
        if (filtered.isEmpty()) return
        val header = TextView(this).apply {
            text = sectionTitle(category, filtered.size)
            setTextColor(getProperTextColor())
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 32, 0, 16)
        }
        container.addView(header)
        for (entryItem in filtered) {
            container.addView(buildRow(entryItem))
        }
    }

    private fun sectionTitle(category: FraudFilter.Category, count: Int): String =
        when (category) {
            FraudFilter.Category.SPAM -> getString(R.string.dsremo_filtered_section_spam, count)
            FraudFilter.Category.PROMOTIONS -> getString(R.string.dsremo_filtered_section_promo, count)
            FraudFilter.Category.INBOX -> getString(R.string.dsremo_filtered_section_inbox, count)
        }

    private fun buildRow(entry: Entry): View {
        val rowContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(rowBackgroundColor(entry.verdict.category))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8; bottomMargin = 8 }
        }
        val senderLine = TextView(this).apply {
            text = getString(R.string.dsremo_filtered_sender_line, entry.address, entry.verdict.score)
            setTextColor(getProperTextColor())
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }
        val bodyLine = TextView(this).apply {
            text = entry.body.take(180)
            setTextColor(getProperTextColor())
            textSize = 13f
            setPadding(0, 6, 0, 6)
        }
        val reasonsLine = TextView(this).apply {
            text = if (entry.verdict.reasons.isEmpty()) {
                getString(R.string.dsremo_verdict_no_reasons)
            } else {
                entry.verdict.reasons.joinToString("\n") { reasonText -> "• $reasonText" }
            }
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
            setPadding(0, 4, 0, 8)
        }
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val openButton = Button(this).apply {
            text = getString(R.string.dsremo_filtered_open_thread)
            setOnClickListener { openThread(entry) }
        }
        val notFraudButton = Button(this).apply {
            text = getString(R.string.dsremo_filtered_not_fraud)
            setOnClickListener { markNotFraud(entry, rowContainer) }
        }
        buttonRow.addView(openButton)
        buttonRow.addView(notFraudButton)

        rowContainer.addView(senderLine)
        rowContainer.addView(bodyLine)
        rowContainer.addView(reasonsLine)
        rowContainer.addView(buttonRow)
        return rowContainer
    }

    private fun rowBackgroundColor(category: FraudFilter.Category): Int = when (category) {
        FraudFilter.Category.SPAM -> Color.parseColor("#3D1F1F")
        FraudFilter.Category.PROMOTIONS -> Color.parseColor("#2F2A1F")
        FraudFilter.Category.INBOX -> Color.parseColor("#1F2F1F")
    }

    private fun openThread(entry: Entry) {
        val intent = Intent(this, ThreadActivity::class.java).apply {
            putExtra(THREAD_ID, entry.threadId)
            putExtra(THREAD_TITLE, entry.address)
        }
        startActivity(intent)
    }

    private fun markNotFraud(entry: Entry, rowView: View) {
        FraudVerdictStore.clear(this, entry.messageId)
        rowView.visibility = View.GONE
        android.widget.Toast.makeText(
            this,
            getString(R.string.dsremo_filtered_marked_not_fraud),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
