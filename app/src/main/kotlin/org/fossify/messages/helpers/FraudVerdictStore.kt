package org.fossify.messages.helpers

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FraudVerdictStore {
    private const val PREFS_NAME = "dsremo_fraud_verdicts"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, messageId: Long, verdict: FraudFilter.Verdict) {
        if (verdict.category == FraudFilter.Category.INBOX && verdict.score == 0) return
        val json = JSONObject()
        json.put("category", verdict.category.name)
        json.put("score", verdict.score)
        json.put("reasons", JSONArray(verdict.reasons))
        prefs(context).edit().putString(messageId.toString(), json.toString()).apply()
    }

    fun get(context: Context, messageId: Long): FraudFilter.Verdict? {
        val raw = prefs(context).getString(messageId.toString(), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val category = FraudFilter.Category.valueOf(json.getString("category"))
            val score = json.getInt("score")
            val reasonArray = json.getJSONArray("reasons")
            val reasons = (0 until reasonArray.length()).map { idx ->
                reasonArray.getString(idx)
            }
            FraudFilter.Verdict(category, score, reasons)
        }.getOrNull()
    }

    fun clear(context: Context, messageId: Long) {
        prefs(context).edit().remove(messageId.toString()).apply()
    }

    fun getAllVerdicts(context: Context): Map<Long, FraudFilter.Verdict> {
        val result = mutableMapOf<Long, FraudFilter.Verdict>()
        val all = prefs(context).all
        for ((key, value) in all) {
            val messageId = key.toLongOrNull() ?: continue
            val raw = value as? String ?: continue
            val verdict = runCatching {
                val json = JSONObject(raw)
                val category = FraudFilter.Category.valueOf(json.getString("category"))
                val score = json.getInt("score")
                val reasonArray = json.getJSONArray("reasons")
                val reasons = (0 until reasonArray.length()).map { idx ->
                    reasonArray.getString(idx)
                }
                FraudFilter.Verdict(category, score, reasons)
            }.getOrNull() ?: continue
            result[messageId] = verdict
        }
        return result
    }
}
