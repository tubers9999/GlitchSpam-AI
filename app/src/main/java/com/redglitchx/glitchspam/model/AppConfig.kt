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
            val raw = prefs.getString("available_models", DEFAULT_MODELS) ?: ""
            return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        set(value) {
            prefs.edit().putString("available_models", value.joinToString(",")).apply()
        }

    companion object {
        // ── NVIDIA NIM Free Endpoint models (no login, endpoint: https://integrate.api.nvidia.com/v1) ──
        // Skipped: deprecated models (cosmos-transfer2.5-2b, llama-nemotron-rerank-vl-1b-v2,
        //          llama-nemotron-embed-vl-1b-v2, nemotron-parse, nemotron-parse-2.0,
        //          llama-guard-4-12b, llama-3.1-nemoguard-8b-content-safety)
        val NVIDIA_NIM_MODELS = listOf(
            // === CHAT / REASONING ===
            "moonshotai/kimi-k3_1-pro",           // 1M context, agentic coding
            "mistralai/mistral-large-4-1t-a52b",  // 1T/52B MoE, multimodal
            "nvidia/nemotron-3-5-bolt-e4b",       // Compact agent/tool-calling
            "nvidia/nemotron-3-5-super-vl-120b-a12b", // Reasoning, agentic, multimodal
            "nvidia/nemotron-3.5-lightning-30b-a3b",  // Fastest 30B MoE
            "meta/muse-glimmer-30b",               // 30B multimodal reasoning
            "stepfun-ai/step-5-preview",           // 600B/27B MoE, 1M ctx
            "openai/gpt-oss-20b",                  // OpenAI MoE, reasoning
            "poolside/laguna-xs-2.1",              // 33B MoE agentic coding
            "z-ai/glm-5-3-flash",                  // 320B/18B MoE multimodal
            "minimaxai/minimax-m3-1-flash",        // Native multimodal MoE
            "deepseek-ai/deepseek-v4.1-flash",     // 552B MoE, 8B active, multimodal
            "google/gemma-4-31b-it",               // Dense 31B, reasoning
            "nvidia/llama-3.1-nemotron-ultra-253b-v1", // Ultra 253B
            "nvidia/nemotron-3-ultra-550b-a55b",   // 550B Mamba-Transformer MoE
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", // Omni-modal reasoning
            "nvidia/nemotron-3-super-120b-a12b",   // Super reasoning model
            "meta/llama-3.2-90b-vision-instruct",  // Vision-language 90B
            "nvidia/riva-translate-4b-instruct-v2",// Translation 37 languages
            "nvidia/ising-calibration-1-5-31b",    // Quantum VLM Gemma 4 31B
            // === EMBEDDING ===
            "nvidia/nemotron-3-5-embed-1b",        // 1B multimodal embedding
            "nvidia/nemotron-3-5-embed-8b",        // 8B multimodal embedding
            "nvidia/nemotron-3-embed-1b",          // 1B embedding for RAG
            // === RERANKING ===
            "nvidia/nemotron-3-5-rerank-8b"        // 8B multimodal reranking
        )

        // ── OpenRouter Free models (no key needed, endpoint: https://openrouter.ai/api/v1) ──
        val OPENROUTER_FREE_MODELS = listOf(
            "nvidia/nemotron-3-5-super-vl:free",
            "nvidia/nemotron-3-5-lightning:free",
            "nvidia/nemotron-3-ultra-550b-a55b:free",
            "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
            "google/gemma-4-31b-it:free",
            "google/gemma-4-26b-a4b-it:free",
            "openrouter/free",
            "inclusionai/ling-3.2-flash:free",
            "baidu/ernie-5.5-flash-preview:free",
            "liquid/lfm-3-1.7b:free",
            "particle-ai/particle-1:free",
            "cohere/north-mini-code:free"
        )

        // Default models list (mixed: popular + NVIDIA NIM free + OpenRouter free)
        val DEFAULT_MODELS = (listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "claude-3-5-sonnet",
            "deepseek-chat"
        ) + NVIDIA_NIM_MODELS.take(10) + OPENROUTER_FREE_MODELS.take(5)).joinToString(",")
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
