package com.redglitchx.glitchspam.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.redglitchx.glitchspam.GlitchApp
import com.redglitchx.glitchspam.model.AppConfig
import com.redglitchx.glitchspam.net.AiClient
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class GlitchAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "GlitchAccessibility"
        var instance: GlitchAccessibilityService? = null
            private set

        var isServiceConnected: Boolean = false
            private set

        // Global event listener for UI updates (Floating HUD & MainActivity)
        var statusListener: ((status: String, sentCount: Int, lastMsg: String) -> Unit)? = null
    }

    private lateinit var config: AppConfig
    private lateinit var aiClient: AiClient
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastProcessedMessage: String = ""
    private var lastSentMessage: String = ""
    private var isCurrentlySending = AtomicBoolean(false)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceConnected = true
        config = AppConfig(applicationContext)
        aiClient = AiClient(config)
        Log.i(TAG, "GlitchSpam Accessibility Service connected.")
        notifyStatus("Active & Hooked", config.totalSentCount, "")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !config.isActive) return

        val packageName = event.packageName?.toString() ?: ""
        if (packageName == applicationContext.packageName) return

        val rootNode = event.source ?: rootInActiveWindow ?: run {
            val appWindow = windows?.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            appWindow?.root
        } ?: return

        // Process message inspection asynchronously to keep accessibility snappy
        serviceScope.launch {
            inspectAndReact(rootNode, packageName)
        }
    }

    private suspend fun inspectAndReact(rootNode: AccessibilityNodeInfo, packageName: String) {
        if (isCurrentlySending.get()) return

        try {
            var latestMessage: String? = null

            // Special high-accuracy resolution for WhatsApp
            if (packageName.contains("whatsapp", ignoreCase = true)) {
                val waMessageNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/message_text").ifEmpty {
                    rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/message_text")
                }
                if (!waMessageNodes.isNullOrEmpty()) {
                    val texts = waMessageNodes.mapNotNull { it.text?.toString()?.trim() }.filter { it.isNotBlank() }
                    latestMessage = texts.lastOrNull { it != lastSentMessage }
                }
            }

            // General resolution across apps
            if (latestMessage == null) {
                val messagesOnScreen = mutableListOf<String>()
                extractAllTexts(rootNode, messagesOnScreen)
                if (messagesOnScreen.isEmpty()) return

                val candidates = messagesOnScreen.filter { text ->
                    val trimmed = text.trim()
                    trimmed.length > 1 &&
                            trimmed != lastSentMessage &&
                            !isSystemOrStatusLabel(trimmed)
                }
                if (candidates.isEmpty()) return
                latestMessage = candidates.last()
            }

            if (latestMessage.isNullOrBlank() || latestMessage == lastProcessedMessage) {
                return
            }

            // Check target filter
            val targetFilter = config.targetFilter.trim()
            val matchesTarget = if (targetFilter.isEmpty()) {
                true // Target ALL incoming messages
            } else {
                latestMessage.contains(targetFilter, ignoreCase = true)
            }

            if (!matchesTarget) return

            // Mark as processed
            lastProcessedMessage = latestMessage
            notifyStatus("Target in $packageName", config.totalSentCount, latestMessage)

            // Trigger reply
            executeRoastAndSpam(rootNode, latestMessage)

        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting screen nodes: ${e.message}")
        }
    }

    private suspend fun executeRoastAndSpam(rootNode: AccessibilityNodeInfo, incomingText: String) {
        if (!isCurrentlySending.compareAndSet(false, true)) return

        try {
            val isTurbo = config.turboSpam
            val burstTotal = if (isTurbo) maxOf(3, config.burstCount) else 1
            val delayMs = config.spamDelayMs

            for (i in 0 until burstTotal) {
                if (!config.isActive) break

                notifyStatus("Preparing message #${i + 1}...", config.totalSentCount, incomingText)

                // Select text: from user's custom file (.txt) if enabled, otherwise AI or preset
                val textToSend = if (config.useCustomFile && config.getCustomLines().isNotEmpty()) {
                    config.getNextCustomLine()
                } else if (config.apiKey.isNotBlank()) {
                    aiClient.generateRoast(incomingText)
                } else {
                    PresetRoasts.getRandomRoast(config.language)
                }
                lastSentMessage = textToSend

                // Find input field on the current active window
                val currentRoot = rootInActiveWindow ?: rootNode
                val inputNode = findInputEditText(currentRoot)

                if (inputNode != null) {
                    // Focus and click the input field
                    inputNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    inputNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    delay(50)

                    // 1. Primary injection via ACTION_SET_TEXT
                    val arguments = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            textToSend
                        )
                    }
                    var textSet = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    // 2. Clipboard paste fallback (vital for React Native apps like Discord)
                    try {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText("glitch_text", textToSend)
                        clipboard?.setPrimaryClip(clip)
                        if (!textSet) {
                            textSet = inputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Clipboard paste fallback: ${e.message}")
                    }

                    // Micro-delay for UI rendering before clicking send
                    delay(150)

                    // Find and click send button
                    val refreshedRoot = rootInActiveWindow ?: currentRoot
                    var sendButton: AccessibilityNodeInfo? = null

                    // Direct WhatsApp Send button check (com.whatsapp:id/send)
                    val waSendList = refreshedRoot.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send").ifEmpty {
                        refreshedRoot.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send")
                    }
                    if (!waSendList.isNullOrEmpty()) {
                        sendButton = waSendList.firstOrNull { it.isClickable }
                    }

                    if (sendButton == null) {
                        sendButton = findSendButton(refreshedRoot)
                    }

                    // Fallback to input's parent hierarchy
                    if (sendButton == null && inputNode.parent != null) {
                        sendButton = findSendButton(inputNode.parent)
                    }

                    if (sendButton != null) {
                        sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        config.incrementSentCount()
                        notifyStatus("Sent #${config.totalSentCount}", config.totalSentCount, textToSend)
                    } else {
                        val clicked = sendButtonFallbackClick(refreshedRoot)
                        if (!clicked) {
                            notifyStatus("Injected #${config.totalSentCount + 1}", config.totalSentCount, textToSend)
                        }
                    }
                }

                if (i < burstTotal - 1) {
                    delay(delayMs)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Execution error: ${e.message}")
        } finally {
            isCurrentlySending.set(false)
        }
    }

    /**
     * Force sends an instant reply right now into whichever input field is open on screen!
     */
    fun forceRoastNow() {
        val rootNode = rootInActiveWindow ?: return
        serviceScope.launch {
            val mockPrompt = if (lastProcessedMessage.isNotBlank()) lastProcessedMessage else "roast"
            executeRoastAndSpam(rootNode, mockPrompt)
        }
    }

    private fun extractAllTexts(node: AccessibilityNodeInfo?, result: MutableList<String>) {
        if (node == null) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val className = node.className?.toString() ?: ""

        if (!className.contains("EditText", ignoreCase = true)) {
            if (!text.isNullOrEmpty()) {
                result.add(text)
            } else if (!desc.isNullOrEmpty() && desc.length > 2 && !desc.contains("avatar", true)) {
                result.add(desc)
            }
        }

        for (i in 0 until node.childCount) {
            extractAllTexts(node.getChild(i), result)
        }
    }

    private fun findInputEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val className = node.className?.toString() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        val isEditableClass = node.isEditable ||
                className.contains("EditText", ignoreCase = true) ||
                viewId.contains("entry") || // WhatsApp
                viewId.contains("chat_input") || // Discord
                viewId.contains("text_input") ||
                viewId.contains("chat_text_edit") || // Telegram
                viewId.contains("input") ||
                desc.contains("message") ||
                desc.contains("mesaj")

        if (isEditableClass && (node.isFocusable || node.isClickable || node.isEditable)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val found = findInputEditText(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val isSendDesc = desc.contains("send") || desc.contains("trimite") ||
                desc.contains("enviar") || desc.contains("envoyer") || desc.contains("senden")
        val isSendId = viewId.contains("send") || viewId.contains("input_send") ||
                viewId.contains("btn_send") || viewId.contains("send_button") ||
                viewId.contains("chat_input_send_button")
        val isSendText = text == "send" || text == "trimite"

        if (node.isClickable && (isSendDesc || isSendId || isSendText)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val found = findSendButton(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun sendButtonFallbackClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val terms = listOf("Send", "Trimite", "send", "trimite", "Enviar", "Envoyer")
        for (term in terms) {
            val list = node.findAccessibilityNodeInfosByText(term)
            if (!list.isNullOrEmpty()) {
                for (item in list) {
                    if (item.isClickable) {
                        item.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        config.incrementSentCount()
                        return true
                    } else if (item.parent?.isClickable == true) {
                        item.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        config.incrementSentCount()
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isSystemOrStatusLabel(text: String): Boolean {
        val lower = text.lowercase().trim()
        val systemKeywords = listOf(
            "online", "typing...", "scrie...", "offline", "yesterday", "ieri",
            "today", "astăzi", "delivered", "read", "seen", "status", "calls", "chats",
            "voice message", "mesaj vocal", "camera", "attach", "atașați", "emoji",
            "audio call", "video call", "apel audio", "apel video", "search", "caută",
            "type a message", "scrie un mesaj", "message", "mesaj", "document", "gallery",
            "galerie", "contact", "location", "locație", "sticker", "stickere", "gif",
            "more options", "mai multe opțiuni", "navigate up", "înapoi", "photo", "poza",
            "forward", "redirecționează", "reply", "răspunde", "glitch", "ai", "gx",
            "view once", "trimite", "send"
        )
        return lower in systemKeywords ||
                systemKeywords.any { lower == it } ||
                text.length > 500
    }

    private fun notifyStatus(status: String, sentCount: Int, lastMsg: String) {
        serviceScope.launch(Dispatchers.Main) {
            statusListener?.invoke(status, sentCount, lastMsg)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "GlitchAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceConnected = false
        serviceScope.cancel()
    }
}
