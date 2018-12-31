/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.preference

import android.content.Context
import android.support.v7.preference.PreferenceViewHolder
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.RadioButton
import android.widget.RadioGroup
import im.vector.R
import org.matrix.androidsdk.rest.model.bingrules.BingRule

class BingRulePreference : VectorPreference {

    /**
     * @return the selected bing rule
     */
    var rule: BingRule? = null
        private set

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        layoutResource = R.layout.vector_preference_bing_rule
    }

    /**
     * @return the bing rule status index
     */
    val ruleStatusIndex: Int
        get() {
            if (null != rule) {
                if (TextUtils.equals(rule!!.ruleId, BingRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS)) {
                    if (rule!!.shouldNotNotify()) {
                        return if (rule!!.isEnabled) {
                            NOTIFICATION_OFF_INDEX
                        } else {
                            NOTIFICATION_ON_INDEX
                        }
                    } else if (rule!!.shouldNotify()) {
                        return NOTIFICATION_NOISY_INDEX
                    }
                }

                if (rule!!.isEnabled) {
                    return if (rule!!.shouldNotNotify()) {
                        NOTIFICATION_OFF_INDEX
                    } else if (null != rule!!.notificationSound) {
                        NOTIFICATION_NOISY_INDEX
                    } else {
                        NOTIFICATION_ON_INDEX
                    }
                }
            }

            return NOTIFICATION_OFF_INDEX
        }

    /**
     * Update the bing rule.
     *
     * @param aBingRule
     */
    fun setBingRule(aBingRule: BingRule) {
        rule = aBingRule
        refreshSummary()
    }

    /**
     * Refresh the summary
     */
    private fun refreshSummary() {
        summary = context.getString(when (ruleStatusIndex) {
            NOTIFICATION_OFF_INDEX -> R.string.notification_off
            NOTIFICATION_ON_INDEX -> R.string.notification_silent
            else -> R.string.notification_noisy
        })
    }

    /**
     * Create a bing rule with the updated required at index.
     *
     * @param index index
     * @return a bing rule with the updated flags / null if there is no update
     */
    fun createRule(index: Int): BingRule? {
        var rule: BingRule? = null

        if (null != this.rule && index != ruleStatusIndex) {
            rule = BingRule(this.rule!!)

            if (TextUtils.equals(rule.ruleId, BingRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS)) {
                when (index) {
                    NOTIFICATION_OFF_INDEX -> {
                        rule.isEnabled = true
                        rule.setNotify(false)
                    }
                    NOTIFICATION_ON_INDEX -> {
                        rule.isEnabled = false
                        rule.setNotify(false)
                    }
                    NOTIFICATION_NOISY_INDEX -> {
                        rule.isEnabled = true
                        rule.setNotify(true)
                        rule.notificationSound = BingRule.ACTION_VALUE_DEFAULT
                    }
                }

                return rule
            }


            if (NOTIFICATION_OFF_INDEX == index) {
                if (TextUtils.equals(this.rule!!.kind, BingRule.KIND_UNDERRIDE)
                        || TextUtils.equals(rule.ruleId, BingRule.RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS)) {
                    rule.setNotify(false)
                } else {
                    rule.isEnabled = false
                }
            } else {
                rule.isEnabled = true
                rule.setNotify(true)
                rule.setHighlight(!TextUtils.equals(this.rule!!.kind, BingRule.KIND_UNDERRIDE)
                        && !TextUtils.equals(rule.ruleId, BingRule.RULE_ID_INVITE_ME)
                        && NOTIFICATION_NOISY_INDEX == index)
                if (NOTIFICATION_NOISY_INDEX == index) {
                    rule.notificationSound = if (TextUtils.equals(rule.ruleId, BingRule.RULE_ID_CALL))
                        BingRule.ACTION_VALUE_RING
                    else
                        BingRule.ACTION_VALUE_DEFAULT
                } else {
                    rule.removeNotificationSound()
                }
            }
        }

        return rule
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val radioGroup = holder.findViewById(R.id.bingPreferenceRadioGroup) as? RadioGroup

        when (ruleStatusIndex) {
            NOTIFICATION_OFF_INDEX -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleOff)
            }
            NOTIFICATION_ON_INDEX -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleOn)
            }
            else -> {
                radioGroup?.check(R.id.bingPreferenceRadioBingRuleNoisy)
            }
        }

        radioGroup?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.bingPreferenceRadioBingRuleOff -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NOTIFICATION_OFF_INDEX)
                }
                R.id.bingPreferenceRadioBingRuleOn -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NOTIFICATION_ON_INDEX)
                }
                R.id.bingPreferenceRadioBingRuleNoisy -> {
                    onPreferenceChangeListener?.onPreferenceChange(this, NOTIFICATION_NOISY_INDEX)
                }
            }
        }

    }


    companion object {

        // index in mRuleStatuses
        private const val NOTIFICATION_OFF_INDEX = 0
        private const val NOTIFICATION_ON_INDEX = 1
        private const val NOTIFICATION_NOISY_INDEX = 2
    }
}