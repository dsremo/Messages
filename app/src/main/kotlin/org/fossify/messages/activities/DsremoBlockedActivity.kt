package org.fossify.messages.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Telephony
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.helpers.BlockedMessageStore
import org.fossify.messages.helpers.THREAD_ID
import org.fossify.messages.helpers.THREAD_TITLE
import org.fossify.messages.workers.DsremoBlocklistRefreshWorker

class DsremoBlockedActivity : SimpleActivity() {

    private data class Row(
        val messageId: Long,
        val threadId: Long,
        val address: String,
        val body: String,
        val date: Long,
        val entry: BlockedMessageStore.Entry,
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
            title = getString(R.string.dsremo_blocked_title)
            setTitleTextColor(getProperTextColor())
            setNavigationIcon(org.fossify.commons.R.drawable.ic_arrow_left_vector)
            setNavigationOnClickListener { finish() }
            navigationIcon?.setTint(getProperTextColor())
            inflateMenu(R.menu.menu_dsremo_blocked)
            setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == R.id.dsremo_refresh_blocklist) {
                    DsremoBlocklistRefreshWorker.refreshNow(this@DsremoBlockedActivity)
                    Toast.makeText(
                        this@DsremoBlockedActivity,
                        getString(R.string.dsremo_refresh_blocklist_started),
                        Toast.LENGTH_SHORT,
                    ).show()
                    true
                } else false
            }
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getProperBackgroundColor())
            addView(
                toolbar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            )
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0, 1f,
                )
            )
        }
        setContentView(rootLayout)

        title = getString(R.string.dsremo_blocked_title)
        loadAndRender()
    }

    private fun loadAndRender() {
        ensureBackgroundThread {
            val rows = collectRows()
            runOnUiThread {
                container.removeAllViews()
                if (rows.isEmpty()) {
                    val emptyView = TextView(this).apply {
                        text = getString(R.string.dsremo_blocked_empty)
                        setTextColor(getProperTextColor())
                        textSize = 15f
                        gravity = Gravity.CENTER
                        setPadding(0, 96, 0, 0)
                    }
                    container.addView(emptyView)
                    Toast.makeText(
                        this,
                        getString(R.string.dsremo_blocked_empty),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@runOnUiThread
                }
                for (row in rows) {
                    container.addView(buildRow(row))
                }
            }
        }
    }

    private fun collectRows(): List<Row> {
        val stored = BlockedMessageStore.all(this)
        if (stored.isEmpty()) return emptyList()
        val rows = mutableListOf<Row>()
        for ((messageId, entry) in stored) {
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
                    null,
                )
            }.getOrNull() ?: continue
            cursor.use { messageCursor ->
                if (messageCursor.moveToFirst()) {
                    rows.add(
                        Row(
                            messageId = messageId,
                            threadId = messageCursor.getLong(3),
                            address = messageCursor.getString(0).orEmpty(),
                            body = messageCursor.getString(1).orEmpty(),
                            date = messageCursor.getLong(2),
                            entry = entry,
                        )
                    )
                }
            }
        }
        return rows.sortedByDescending { row -> row.date }
    }

    private fun buildRow(row: Row): View {
        val rowContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(Color.parseColor("#3D1F1F"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8; bottomMargin = 8 }
        }
        val senderLine = TextView(this).apply {
            text = "${row.address} — ${row.entry.host}"
            setTextColor(getProperTextColor())
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }
        val bodyLine = TextView(this).apply {
            text = row.body.take(180)
            setTextColor(getProperTextColor())
            textSize = 13f
            setPadding(0, 6, 0, 6)
        }
        val humanDate = DateUtils.getRelativeDateTimeString(
            this,
            row.entry.timestamp,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            0,
        ).toString()
        val reasonLine = TextView(this).apply {
            text = getString(R.string.dsremo_blocked_source_line, row.entry.source, humanDate)
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
            setPadding(0, 4, 0, 8)
        }
        val buttonRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val openButton = Button(this).apply {
            text = getString(R.string.dsremo_blocked_open_thread)
            setOnClickListener { openThread(row) }
        }
        val ignoreButton = Button(this).apply {
            text = getString(R.string.dsremo_blocked_ignore)
            setOnClickListener { ignoreRow(row, rowContainer) }
        }
        buttonRow.addView(openButton)
        buttonRow.addView(ignoreButton)

        rowContainer.addView(senderLine)
        rowContainer.addView(bodyLine)
        rowContainer.addView(reasonLine)
        rowContainer.addView(buttonRow)
        return rowContainer
    }

    private fun openThread(row: Row) {
        val intent = Intent(this, ThreadActivity::class.java).apply {
            putExtra(THREAD_ID, row.threadId)
            putExtra(THREAD_TITLE, row.address)
        }
        startActivity(intent)
    }

    private fun ignoreRow(row: Row, rowView: View) {
        BlockedMessageStore.clear(this, row.messageId)
        rowView.visibility = View.GONE
    }
}
