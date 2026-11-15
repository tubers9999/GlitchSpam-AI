package com.redglitchx.glitchspam.model

import android.content.Context
import android.content.SharedPreferences

class AppConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("glitch_spam_prefs", Context.MODE_PRIVATE)

    var apiUrl: String
        get() = prefs.getString("api_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
        set(value) = prefs.edit().putString("api_url", value.trimEnd('/')).apply()

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString("selected_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString("selected_model", value.trim()).apply()

    var availableModels: List<String>
        get() {
            val raw = prefs.getString("available_models", "gpt-4o-mini,gpt-4o,gpt-3.5-turbo,claude-3-5-sonnet,deepseek-chat") ?: ""
            return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        set(value) {
            prefs.edit().putString("available_models", value.joinToString(",")).apply()
        }

    var bypassEnabled: Boolean
        get() = prefs.getBoolean("bypass_enabled", true)
        set(value) = prefs.edit().putBoolean("bypass_enabled", value).apply()

    var jailbreakPreset: Int
        get() = prefs.getInt("jailbreak_preset", 0)
        set(value) = prefs.edit().putInt("jailbreak_preset", value).apply()

    var customJailbreak: String
        get() = prefs.getString("custom_jailbreak", "") ?: ""
        set(value) = prefs.edit().putString("custom_jailbreak", value).apply()

    var language: String
        get() = prefs.getString("language", "Română") ?: "Română"
        set(value) = prefs.edit().putString("language", value).apply()

    var tone: String
        get() = prefs.getString("tone", "Brutal Roast (Închidere & Încurătură)") ?: "Brutal Roast (Închidere & Încurătură)"
        set(value) = prefs.edit().putString("tone", value).apply()

    var targetFilter: String
        get() = prefs.getString("target_filter", "") ?: ""
        set(value) = prefs.edit().putString("target_filter", value.trim()).apply()

    var turboSpam: Boolean
        get() = prefs.getBoolean("turbo_spam", false)
        set(value) = prefs.edit().putBoolean("turbo_spam", value).apply()

    var spamDelayMs: Long
        get() = prefs.getLong("spam_delay_ms", 300L)
        set(value) = prefs.edit().putLong("spam_delay_ms", value).apply()

    var burstCount: Int
        get() = prefs.getInt("burst_count", 1)
        set(value) = prefs.edit().putInt("burst_count", value).apply()

    var shakeToKill: Boolean
        get() = prefs.getBoolean("shake_to_kill", true)
        set(value) = prefs.edit().putBoolean("shake_to_kill", value).apply()

    var isActive: Boolean
        get() = prefs.getBoolean("is_active", false)
        set(value) = prefs.edit().putBoolean("is_active", value).apply()

    var totalSentCount: Int
        get() = prefs.getInt("total_sent_count", 0)
        set(value) = prefs.edit().putInt("total_sent_count", value).apply()

    fun incrementSentCount() {
        totalSentCount += 1
    }

    fun resetSentCount() {
        totalSentCount = 0
    }
}
