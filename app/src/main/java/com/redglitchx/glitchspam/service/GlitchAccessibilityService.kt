package com.redglitchx.glitchspam.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

        val rootNode = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: ""

        // Process message inspection asynchronously to keep accessibility snappy
        serviceScope.launch {
            inspectAndReact(rootNode, packageName)
        }
    }

    private suspend fun inspectAndReact(rootNode: AccessibilityNodeInfo, packageName: String) {
        if (isCurrentlySending.get()) return

        try {
            // Find recent incoming message on screen
            val messagesOnScreen = mutableListOf<String>()
            extractAllTexts(rootNode, messagesOnScreen)

            if (messagesOnScreen.isEmpty()) return

            // Filter out system UI labels and own sent message
            val candidates = messagesOnScreen.filter { text ->
                val trimmed = text.trim()
                trimmed.length > 1 &&
                        trimmed != lastSentMessage &&
                        !isSystemOrStatusLabel(trimmed)
            }

            if (candidates.isEmpty()) return
            val latestMessage = candidates.last()

            // Check if this message was already responded to
            if (latestMessage == lastProcessedMessage) {
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
            notifyStatus("Target Detected in $packageName", config.totalSentCount, latestMessage)

            // Trigger roast and spam
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

                notifyStatus("Generating AI Roast #${i + 1}...", config.totalSentCount, incomingText)

                // Generate AI comeback or fetch roast
                val roastText = aiClient.generateRoast(incomingText)
                lastSentMessage = roastText

                // Find input field on the current active window
                val currentRoot = rootInActiveWindow ?: rootNode
                val inputNode = findInputEditText(currentRoot)

                if (inputNode != null) {
                    // Inject text
                    val arguments = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            roastText
                        )
                    }
                    val textSet = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

                    if (textSet) {
                        // Small micro-delay for UI rendering before clicking send
                        delay(60)

                        // Find and click send button
                        val refreshedRoot = rootInActiveWindow ?: currentRoot
                        val sendButton = findSendButton(refreshedRoot)
                        if (sendButton != null) {
                            sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            config.incrementSentCount()
                            notifyStatus("Sent Roast #${config.totalSentCount}", config.totalSentCount, roastText)
                        } else {
                            // Fallback: try pressing ACTION_CLICK on input if send button was not distinct
                            sendButtonFallbackClick(refreshedRoot)
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
     * Force sends an instant roast right now into whichever input field is open on screen!
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
        if (!text.isNullOrEmpty() && node.className != "android.widget.EditText") {
            result.add(text)
        }

        for (i in 0 until node.childCount) {
            extractAllTexts(node.getChild(i), result)
        }
    }

    private fun findInputEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isEditable || node.className == "android.widget.EditText") {
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
                viewId.contains("btn_send") || viewId.contains("send_button")
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

    private fun sendButtonFallbackClick(node: AccessibilityNodeInfo?) {
        if (node == null) return
        val list = node.findAccessibilityNodeInfosByText("Send")
        if (!list.isNullOrEmpty()) {
            list[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            config.incrementSentCount()
            return
        }
        val listRo = node.findAccessibilityNodeInfosByText("Trimite")
        if (!listRo.isNullOrEmpty()) {
            listRo[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            config.incrementSentCount()
        }
    }

    private fun isSystemOrStatusLabel(text: String): Boolean {
        val lower = text.lowercase()
        return lower in listOf(
            "online", "typing...", "scrie...", "offline", "yesterday", "ieri",
            "today", "astăzi", "delivered", "read", "seen", "status", "calls", "chats"
        ) || text.length > 500
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
