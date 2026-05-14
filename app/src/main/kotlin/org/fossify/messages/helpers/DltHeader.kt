package org.fossify.messages.helpers

import java.util.regex.Pattern

data class DltHeader(
    val raw: String,
    val operatorChar: Char?,
    val circleChar: Char?,
    val peName: String?,
    val category: Category,
) {
    enum class Category { PROMOTIONAL, SERVICE, TRANSACTIONAL, GOVERNMENT, UNKNOWN, NOT_DLT }

    val isDlt: Boolean get() = category != Category.NOT_DLT
    val isCommercialPromotional: Boolean get() = category == Category.PROMOTIONAL
    val isTransactional: Boolean get() = category == Category.TRANSACTIONAL
    val isGovernment: Boolean get() = category == Category.GOVERNMENT

    companion object {
        private val DLT_PATTERN = Pattern.compile(
            "^([A-Za-z]{2})-([A-Za-z0-9]{2,6})(?:-([PSTG]))?$"
        )
        private val RAW_MOBILE_PATTERN = Pattern.compile("^\\+?91?[6-9]\\d{9}$")

        fun parse(sender: String): DltHeader {
            val trimmed = sender.trim()
            if (trimmed.isEmpty()) {
                return DltHeader(sender, null, null, null, Category.NOT_DLT)
            }
            val matcher = DLT_PATTERN.matcher(trimmed)
            if (!matcher.matches()) {
                return DltHeader(sender, null, null, null, Category.NOT_DLT)
            }
            val opCircle = matcher.group(1)!!.uppercase()
            val pe = matcher.group(2)!!.uppercase()
            val suffix = matcher.group(3)?.uppercase()
            val category = when (suffix) {
                "P" -> Category.PROMOTIONAL
                "S" -> Category.SERVICE
                "T" -> Category.TRANSACTIONAL
                "G" -> Category.GOVERNMENT
                null -> Category.UNKNOWN
                else -> Category.UNKNOWN
            }
            return DltHeader(sender, opCircle[0], opCircle[1], pe, category)
        }

        fun isRawMobile(sender: String): Boolean =
            RAW_MOBILE_PATTERN.matcher(sender.trim()).matches()
    }
}
