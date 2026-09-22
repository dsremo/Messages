package org.fossify.messages.helpers

import android.content.Context

object DsremoRuleToggles {

    private const val PREFS = "dsremo_rule_toggles"

    enum class Rule(val key: String, val label: String, val defaultOn: Boolean) {
        RAW_MOBILE_BANK("raw_mobile_bank",
            "10-digit mobile impersonating bank/UPI", true),
        SUFFIX_MISMATCH("suffix_mismatch",
            "Promotional -P suffix but banking/KYC content", true),
        SHORTENER("shortener",
            "URL shortener (bit.ly / tinyurl / cutt.ly / wa.me / etc.)", true),
        SUSPICIOUS_TLD("suspicious_tld",
            "Suspicious TLD (.top / .xyz / .click / .loan / etc.)", true),
        APK_TRAP("apk_trap",
            "APK / executable file mention (malware drop)", true),
        URL_ONLY_BODY("url_only_body",
            "Body is just a bare URL", true),
        UPI_REFUND_SCAM("upi_refund",
            "UPI 'refund / wrong-transfer' scam pattern", true),
        PARCEL_SCAM("parcel_scam",
            "Parcel / courier / customs scam pattern", true),
        ELECTRICITY_SCAM("electricity_scam",
            "Electricity disconnect-tonight scam", true),
        TASK_JOB_SCAM("task_job_scam",
            "Earn-daily / task / part-time-work scam", true),
        OTP_SHARE_FRAUD("otp_share_fraud",
            "Asks user to 'share OTP'", true),
        CBI_ARREST("cbi_arrest",
            "Digital arrest / CBI / FedEx narcotics threat", true),
        MARKETING_WORDS("marketing_words",
            "Marketing jargon (SALE / OFFER / FREE / WIN / FLAT)", true),
        RS_AMOUNT_OFFER("rs_amount_offer",
            "Money amount + offer language", true),
        TANDC("tandc",
            "'T&C apply' advertiser tell", true),
        UNSUBSCRIBE("unsubscribe",
            "Has 'unsubscribe' / STOP-to-shortcode tell", true),
        CLICK_HERE("click_here",
            "'Click here / tap below' funnel language", true),
        URGENCY_URL("urgency_url",
            "Urgency language combined with URL", true),
        ALL_CAPS("all_caps",
            "ALL-CAPS density", true),
        MMS_ILLEGIBLE("mms_illegible",
            "MMS with illegible heading + attachment-only (ad)", true);

        companion object {
            fun all() = entries.toList()
        }
    }

    fun isEnabled(context: Context, rule: Rule): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(rule.key, rule.defaultOn)
    }

    fun setEnabled(context: Context, rule: Rule, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(rule.key, value).apply()
    }
}
