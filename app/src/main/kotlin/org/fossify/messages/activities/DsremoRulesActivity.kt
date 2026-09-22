package org.fossify.messages.activities

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.messages.R
import org.fossify.messages.helpers.DsremoRuleToggles

class DsremoRulesActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.dsremo_rules_title)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getProperBackgroundColor())
            setPadding(0, 24, 0, 24)
        }
        val toolbar = androidx.appcompat.widget.Toolbar(this).apply {
            setBackgroundColor(getProperBackgroundColor())
            title = getString(R.string.dsremo_rules_title)
            setTitleTextColor(getProperTextColor())
            setNavigationIcon(org.fossify.commons.R.drawable.ic_arrow_left_vector)
            setNavigationOnClickListener { finish() }
            navigationIcon?.setTint(getProperTextColor())
        }
        val intro = TextView(this).apply {
            text = getString(R.string.dsremo_rules_intro)
            setTextColor(getProperTextColor())
            textSize = 13f
            setPadding(48, 16, 48, 24)
            alpha = 0.7f
        }
        container.addView(toolbar)
        container.addView(intro)

        for (rule in DsremoRuleToggles.Rule.all()) {
            container.addView(buildRow(rule))
        }
        setContentView(ScrollView(this).apply { addView(container) })
    }

    private fun buildRow(rule: DsremoRuleToggles.Rule): androidx.appcompat.widget.SwitchCompat {
        val sw = androidx.appcompat.widget.SwitchCompat(this).apply {
            text = rule.label
            isChecked = DsremoRuleToggles.isEnabled(this@DsremoRulesActivity, rule)
            setTextColor(getProperTextColor())
            textSize = 14f
            setPadding(48, 16, 48, 16)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnCheckedChangeListener { _, isChecked ->
                DsremoRuleToggles.setEnabled(this@DsremoRulesActivity, rule, isChecked)
            }
        }
        return sw
    }
}
