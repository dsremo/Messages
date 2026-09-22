package org.fossify.messages.helpers

import androidx.annotation.DrawableRes
import org.fossify.messages.R

object BankSenderIcon {

    private val bankPatterns: List<Regex> = listOf(
        Regex("(?i)\\bSBI(?:UPI|INB|PSG|CRD|BNK|OTP)?\\b"),
        Regex("(?i)\\bHDFC(?:BK|BN|LI|CC)?\\b"),
        Regex("(?i)\\bICICI(?:B|BK|CC|CR)?\\b"),
        Regex("(?i)\\bAXIS(?:BK|BN|CC)?\\b"),
        Regex("(?i)\\bKOTAK(?:BK|B|CC)?\\b"),
        Regex("(?i)\\bPNB(?:SMS|BNK)?\\b"),
        Regex("(?i)\\bBOB(?:SMS|BNK|TXN)?\\b"),
        Regex("(?i)\\bBOI(?:SMS|IND)?\\b"),
        Regex("(?i)\\bCANBNK\\b"),
        Regex("(?i)\\bIDBIBK\\b"),
        Regex("(?i)\\bIDFCFB\\b"),
        Regex("(?i)\\bYESBNK\\b"),
        Regex("(?i)\\bINDBNK\\b"),
        Regex("(?i)\\bUNIONB\\b"),
        Regex("(?i)\\bCENTBK\\b"),
        Regex("(?i)\\bUCOBNK\\b"),
        Regex("(?i)\\bRBLBNK\\b"),
        Regex("(?i)\\bFEDBNK\\b"),
        Regex("(?i)\\bDBSBNK\\b"),
        Regex("(?i)\\bSCBANK\\b"),
        Regex("(?i)\\bCITIBK\\b"),
        Regex("(?i)\\bAMEXIN\\b"),
        Regex("(?i)\\bAUFINB\\b"),
        Regex("(?i)\\bPYTMBK\\b"),
        Regex("(?i)\\bJIOPMT\\b")
    )

    private val dltPrefixRegex = Regex("^(?i)(?:[A-Z]{2}-)?([A-Z0-9]+)$")

    @DrawableRes
    fun iconFor(senderId: String?, senderName: String?): Int? {
        val candidates = listOfNotNull(senderId, senderName)
        for (candidate in candidates) {
            if (candidate.isBlank()) continue
            val normalized = extractSenderCore(candidate)
            if (bankPatterns.any { it.containsMatchIn(candidate) || it.containsMatchIn(normalized) }) {
                return R.drawable.ic_bank_dsremo_vector
            }
        }
        return null
    }

    private fun extractSenderCore(rawSenderId: String): String {
        val trimmed = rawSenderId.trim()
        val match = dltPrefixRegex.matchEntire(trimmed)
        return match?.groupValues?.getOrNull(1) ?: trimmed
    }
}
