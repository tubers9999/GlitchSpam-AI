package com.redglitchx.glitchspam

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.redglitchx.glitchspam.data.PresetRoasts
import com.redglitchx.glitchspam.model.AppConfig
import com.redglitchx.glitchspam.net.AiClient
import com.redglitchx.glitchspam.service.GlitchAccessibilityService
import com.redglitchx.glitchspam.service.GlitchOverlayService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var config: AppConfig
    private lateinit var aiClient: AiClient

    // UI elements
    private lateinit var tvAccStatus: TextView
    private lateinit var btnGrantAcc: Button
    private lateinit var tvOverlayStatus: TextView
    private lateinit var btnGrantOverlay: Button
    private lateinit var btnLaunchOverlay: Button

    private lateinit var etApiUrl: EditText
    private lateinit var etApiKey: EditText
    private lateinit var btnAutoDetect: Button
    private lateinit var spModels: Spinner

    private lateinit var btnPresetOpenAi: Button
    private lateinit var btnPresetOpenRouter: Button
    private lateinit var btnPresetGroq: Button
    private lateinit var btnPresetDeepSeek: Button
    private lateinit var btnPresetNvidiaFree: Button
    private lateinit var btnPresetOpenRouterFree: Button

    private lateinit var swBypass: SwitchCompat
    private lateinit var spJailbreak: Spinner

    private lateinit var spTone: Spinner
    private lateinit var spLanguage: Spinner
    private lateinit var etTargetConfig: EditText

    private lateinit var tvDelayDisplay: TextView
    private lateinit var sbDelay: SeekBar
    private lateinit var swShakeKill: SwitchCompat

    private lateinit var etTestInput: EditText
    private lateinit var btnTestRoast: Button
    private lateinit var tvTerminal: TextView
    private lateinit var btnSaveSettings: Button

    // Custom File (.txt) Script Mode
    private lateinit var swUseCustomFile: SwitchCompat
    private lateinit var btnLoadCustomFile: Button
    private lateinit var btnClearCustomFile: Button
    private lateinit var etCustomFileContent: EditText
    private lateinit var tvCustomLinesCount: TextView

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    etCustomFileContent.setText(text)
                    config.customFileContent = text
                    config.useCustomFile = true
                    swUseCustomFile.isChecked = true
                    updateLoadedLinesCount()
                    logTerminal("Loaded ${config.getCustomLines().size} messages from file.")
                    Toast.makeText(this, "Loaded ${config.getCustomLines().size} messages from file!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                logTerminal("[ERROR] Failed to read file: ${e.message}")
                Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        config = AppConfig(this)
        aiClient = AiClient(config)

        bindViews()
        setupListeners()
        loadSavedConfig()
        checkPermissions()
        setupLiveTerminal()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun bindViews() {
        tvAccStatus = findViewById(R.id.tv_acc_status)
        btnGrantAcc = findViewById(R.id.btn_grant_accessibility)
        tvOverlayStatus = findViewById(R.id.tv_overlay_status)
        btnGrantOverlay = findViewById(R.id.btn_grant_overlay)
        btnLaunchOverlay = findViewById(R.id.btn_launch_overlay)

        etApiUrl = findViewById(R.id.et_api_url)
        etApiKey = findViewById(R.id.et_api_key)
        btnAutoDetect = findViewById(R.id.btn_auto_detect_models)
        spModels = findViewById(R.id.sp_models)

        btnPresetOpenAi = findViewById(R.id.btn_preset_openai)
        btnPresetOpenRouter = findViewById(R.id.btn_preset_openrouter)
        btnPresetGroq = findViewById(R.id.btn_preset_groq)
        btnPresetDeepSeek = findViewById(R.id.btn_preset_deepseek)
        btnPresetNvidiaFree = findViewById(R.id.btn_preset_nvidia_free)
        btnPresetOpenRouterFree = findViewById(R.id.btn_preset_openrouter_free)

        swBypass = findViewById(R.id.sw_bypass_mode)
        spJailbreak = findViewById(R.id.sp_jailbreak_presets)

        spTone = findViewById(R.id.sp_tone)
        spLanguage = findViewById(R.id.sp_language)
        etTargetConfig = findViewById(R.id.et_target_config)

        tvDelayDisplay = findViewById(R.id.tv_delay_display)
        sbDelay = findViewById(R.id.sb_delay)
        swShakeKill = findViewById(R.id.sw_shake_kill)

        etTestInput = findViewById(R.id.et_test_input)
        btnTestRoast = findViewById(R.id.btn_test_roast)
        tvTerminal = findViewById(R.id.tv_terminal_output)
        btnSaveSettings = findViewById(R.id.btn_save_settings)

        swUseCustomFile = findViewById(R.id.sw_use_custom_file)
        btnLoadCustomFile = findViewById(R.id.btn_load_custom_file)
        btnClearCustomFile = findViewById(R.id.btn_clear_custom_file)
        etCustomFileContent = findViewById(R.id.et_custom_file_content)
        tvCustomLinesCount = findViewById(R.id.tv_custom_lines_count)
    }

    private fun setupListeners() {
        btnGrantAcc.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted!", Toast.LENGTH_SHORT).show()
            }
        }

        btnLaunchOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Floating Overlay permission first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }

            saveConfig()
            val serviceIntent = Intent(this, GlitchOverlayService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                logTerminal("Floating Bubble & HUD launched successfully.")
                Toast.makeText(this, "Glitch HUD Bubble launched!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                logTerminal("[ERROR] Failed to start overlay: ${e.message}")
                Toast.makeText(this, "Error starting overlay: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Endpoint Presets
        btnPresetOpenAi.setOnClickListener {
            etApiUrl.setText("https://api.openai.com/v1")
            saveConfig()
            logTerminal("Preset loaded: OpenAI API")
        }

        btnPresetOpenRouter.setOnClickListener {
            etApiUrl.setText("https://openrouter.ai/api/v1")
            saveConfig()
            logTerminal("Preset loaded: OpenRouter API")
        }

        btnPresetGroq.setOnClickListener {
            etApiUrl.setText("https://api.groq.com/openai/v1")
            saveConfig()
            logTerminal("Preset loaded: Groq Cloud API (Ultra-Fast)")
        }

        btnPresetDeepSeek.setOnClickListener {
            etApiUrl.setText("https://api.deepseek.com/v1")
            saveConfig()
            logTerminal("Preset loaded: DeepSeek API")
        }

        // ── FREE / No-Key Presets ──
        btnPresetNvidiaFree.setOnClickListener {
            etApiUrl.setText("https://integrate.api.nvidia.com/v1")
            etApiKey.setText("")
            // Load NVIDIA NIM models into spinner immediately
            val nimModels = AppConfig.NVIDIA_NIM_MODELS
            config.availableModels = nimModels
            setupModelSpinner(nimModels, nimModels.firstOrNull() ?: "")
            saveConfig()
            logTerminal("[PRESET] NVIDIA NIM Free Playground — ${nimModels.size} modele fără login!")
            logTerminal("[INFO] Key: lăsat gol (nvapi-guest automat). Testează cu Auto-Detect.")
            Toast.makeText(this, "NVIDIA NIM Free: ${nimModels.size} modele încărcate!", Toast.LENGTH_SHORT).show()
        }

        btnPresetOpenRouterFree.setOnClickListener {
            etApiUrl.setText("https://openrouter.ai/api/v1")
            etApiKey.setText("")
            val freeModels = AppConfig.OPENROUTER_FREE_MODELS
            config.availableModels = freeModels
            setupModelSpinner(freeModels, freeModels.firstOrNull() ?: "")
            saveConfig()
            logTerminal("[PRESET] OpenRouter FREE — ${freeModels.size} modele gratuite fără key!")
            logTerminal("[INFO] Modele :free — NVIDIA Nemotron, Gemma 4, Liquid LFM, etc.")
            Toast.makeText(this, "OpenRouter Free: ${freeModels.size} modele gratuite!", Toast.LENGTH_SHORT).show()
        }

        // Auto-Detect Models
        btnAutoDetect.setOnClickListener {
            saveConfig()
            val url = etApiUrl.text.toString().trim()
            val key = etApiKey.text.toString().trim()
            val isNvidiaFree = url.contains("integrate.api.nvidia.com")
            val isOpenRouterFree = url.contains("openrouter.ai") && key.isBlank()

            if (key.isBlank() && !isNvidiaFree && !isOpenRouterFree) {
                logTerminal("[WARN] No API key provided. Using offline models.")
                Toast.makeText(this, "Enter an API Key to query remote models!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isNvidiaFree && key.isBlank()) {
                logTerminal("[DETECT] NVIDIA NIM Free — se foloseste nvapi-guest token automat...")
            } else if (isOpenRouterFree) {
                logTerminal("[DETECT] OpenRouter — se cauta modele :free publice...")
            } else {
                logTerminal("[DETECT] Querying $url/models ...")
            }
            btnAutoDetect.isEnabled = false
            btnAutoDetect.text = "DETECTING..."

            lifecycleScope.launch {
                val result = aiClient.fetchAvailableModels()
                btnAutoDetect.isEnabled = true
                btnAutoDetect.text = "🔍 AUTO-DETECT MODELS"

                result.onSuccess { models ->
                    logTerminal("[SUCCESS] Detected ${models.size} models from server!")
                    config.availableModels = models
                    setupModelSpinner(models, models.firstOrNull() ?: "")
                    Toast.makeText(this@MainActivity, "Found ${models.size} models!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    logTerminal("[ERROR] Failed to fetch models: ${err.message}")
                    Toast.makeText(this@MainActivity, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Delay slider
        sbDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actual = maxOf(50, progress)
                tvDelayDisplay.text = "Spam Delay: $actual ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                config.spamDelayMs = maxOf(50, sbDelay.progress).toLong()
            }
        })

        // Test Roast
        btnTestRoast.setOnClickListener {
            saveConfig()
            val input = etTestInput.text.toString().ifBlank { "ce faci măi fraiere" }
            logTerminal("[TEST INPUT] \"$input\"")
            logTerminal("[AI] Thinking comeback with model '${config.selectedModel}'...")

            btnTestRoast.isEnabled = false
            lifecycleScope.launch {
                val roast = aiClient.generateRoast(input)
                btnTestRoast.isEnabled = true
                logTerminal("[ROAST OUTPUT] -> \"$roast\"")
            }
        }

        btnSaveSettings.setOnClickListener {
            saveConfig()
            logTerminal("[CONFIG] All settings successfully saved to disk.")
            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
        }

        // Custom File (.txt) Script Mode Listeners
        btnLoadCustomFile.setOnClickListener {
            openDocumentLauncher.launch("*/*")
        }

        btnClearCustomFile.setOnClickListener {
            etCustomFileContent.setText("")
            config.customFileContent = ""
            updateLoadedLinesCount()
            logTerminal("Custom messages cleared.")
        }

        swUseCustomFile.setOnCheckedChangeListener { _, isChecked ->
            config.useCustomFile = isChecked
            logTerminal("Custom file mode: ${if (isChecked) "ACTIVE" else "OFF"}")
        }

        etCustomFileContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                config.customFileContent = s?.toString() ?: ""
                updateLoadedLinesCount()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadSavedConfig() {
        etApiUrl.setText(config.apiUrl)
        etApiKey.setText(config.apiKey)
        etTargetConfig.setText(config.targetFilter)

        swBypass.isChecked = config.bypassEnabled
        swShakeKill.isChecked = config.shakeToKill

        swUseCustomFile.isChecked = config.useCustomFile
        etCustomFileContent.setText(config.customFileContent)
        updateLoadedLinesCount()

        sbDelay.progress = config.spamDelayMs.toInt()
        tvDelayDisplay.text = "Spam Delay: ${config.spamDelayMs} ms"

        // Spinners
        val jailbreakPresets = listOf(
            "Romanian Street Savage (Unhinged)",
            "Maximum Brutal DAN Bypass",
            "Toxic Competitive Gamer Trash-Talk",
            "Custom User Directive"
        )
        val jbAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jailbreakPresets)
        spJailbreak.adapter = jbAdapter
        spJailbreak.setSelection(config.jailbreakPreset.coerceIn(0, jailbreakPresets.size - 1))

        val tones = listOf(
            "Brutal Roast (Închidere & Încurătură)",
            "Savage Mockery",
            "Toxic Gamer",
            "Sarcastic Troll"
        )
        val toneAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tones)
        spTone.adapter = toneAdapter
        val toneIndex = tones.indexOf(config.tone).let { if (it >= 0) it else 0 }
        spTone.setSelection(toneIndex)

        val languages = listOf("Română", "English", "Español", "Deutsch", "Français", "Auto-Detect")
        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spLanguage.adapter = langAdapter
        val langIndex = languages.indexOf(config.language).let { if (it >= 0) it else 0 }
        spLanguage.setSelection(langIndex)

        setupModelSpinner(config.availableModels, config.selectedModel)
    }

    private fun setupModelSpinner(models: List<String>, selected: String) {
        val list = if (models.isEmpty()) {
            listOf("gpt-4o-mini", "gpt-4o", "claude-3-5-sonnet", "deepseek-chat")
        } else {
            models
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
        spModels.adapter = adapter
        val idx = list.indexOf(selected).let { if (it >= 0) it else 0 }
        spModels.setSelection(idx)
    }

    private fun saveConfig() {
        config.apiUrl = etApiUrl.text.toString().trim()
        config.apiKey = etApiKey.text.toString().trim()
        config.selectedModel = spModels.selectedItem?.toString() ?: "gpt-4o-mini"
        config.targetFilter = etTargetConfig.text.toString().trim()
        config.bypassEnabled = swBypass.isChecked
        config.jailbreakPreset = spJailbreak.selectedItemPosition
        config.tone = spTone.selectedItem?.toString() ?: "Brutal Roast (Închidere & Încurătură)"
        config.language = spLanguage.selectedItem?.toString() ?: "Română"
        config.spamDelayMs = maxOf(50, sbDelay.progress).toLong()
        config.shakeToKill = swShakeKill.isChecked
        config.useCustomFile = swUseCustomFile.isChecked
        config.customFileContent = etCustomFileContent.text.toString().trim()
    }

    private fun updateLoadedLinesCount() {
        val count = config.getCustomLines().size
        tvCustomLinesCount.text = "$count messages loaded"
    }

    private fun checkPermissions() {
        val isAccGranted = isAccessibilityServiceEnabled(this, GlitchAccessibilityService::class.java)
        if (isAccGranted) {
            tvAccStatus.text = "Status: Active"
            tvAccStatus.setTextColor(Color.parseColor("#22C55E"))
            btnGrantAcc.text = "Enabled"
            btnGrantAcc.isEnabled = false
        } else {
            tvAccStatus.text = "Status: Disabled"
            tvAccStatus.setTextColor(Color.parseColor("#A1A1AA"))
            btnGrantAcc.text = "Enable"
            btnGrantAcc.isEnabled = true
        }

        val isOverlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        if (isOverlayGranted) {
            tvOverlayStatus.text = "Status: Granted"
            tvOverlayStatus.setTextColor(Color.parseColor("#22C55E"))
            btnGrantOverlay.text = "Granted"
            btnGrantOverlay.isEnabled = false
        } else {
            tvOverlayStatus.text = "Status: Required for HUD"
            tvOverlayStatus.setTextColor(Color.parseColor("#A1A1AA"))
            btnGrantOverlay.text = "Grant"
            btnGrantOverlay.isEnabled = true
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val expectedComponentName = "${context.packageName}/${service.name}"
        for (info in enabledServices) {
            if (info.id.contains(expectedComponentName) || info.id.contains(service.simpleName)) {
                return true
            }
        }
        return false
    }

    private fun setupLiveTerminal() {
        logTerminal("[CORE] GlitchSpam Engine Initialized by redglitchx.")
        logTerminal("[CORE] Standby for WhatsApp, Discord, Telegram, Messenger automation.")

        GlitchAccessibilityService.statusListener = { status, sentCount, lastMsg ->
            runOnUiThread {
                logTerminal("[EVENT] $status | Sent: $sentCount ${if (lastMsg.isNotBlank()) " | \"$lastMsg\"" else ""}")
            }
        }
    }

    private fun logTerminal(line: String) {
        val timestamp = timeFormat.format(Date())
        val formatted = "[$timestamp] $line\n${tvTerminal.text}"
        tvTerminal.text = formatted
    }
}
