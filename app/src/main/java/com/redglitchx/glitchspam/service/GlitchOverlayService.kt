package com.redglitchx.glitchspam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.redglitchx.glitchspam.R
import com.redglitchx.glitchspam.model.AppConfig
import com.redglitchx.glitchspam.util.ShakeDetector
import kotlin.math.abs

class GlitchOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "glitch_overlay_channel"
        const val NOTIFICATION_ID = 1337
        var isOverlayRunning = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var config: AppConfig
    private var shakeDetector: ShakeDetector? = null

    private var bubbleView: View? = null
    private var windowView: View? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var isWindowExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isOverlayRunning = true
        config = AppConfig(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForegroundServiceNotification()
        setupShakeDetector()
        createFloatingBubble()

        // Hook into accessibility service status updates
        GlitchAccessibilityService.statusListener = { status, sentCount, lastMsg ->
            updateHudStats(status, sentCount)
        }
    }

    private fun startForegroundServiceNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "GlitchSpam Overlay Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps GlitchSpam floating HUD bubble active"
                }
                val nm = getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(channel)
            }

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GlitchSpam AI HUD Active")
                .setContentText("Tap floating bubble on screen to control HUD")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("GlitchOverlay", "Error starting foreground notification: ${e.message}", e)
        }
    }

    private fun setupShakeDetector() {
        if (config.shakeToKill) {
            shakeDetector = ShakeDetector(this) {
                // Emergency panic kill!
                config.isActive = false
                vibrateAlert()
                updateBubbleStatus(false)
                updateWindowButtons()
            }
            shakeDetector?.start()
        }
    }

    private fun vibrateAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }

    private fun createFloatingBubble() {
        val inflater = LayoutInflater.from(this)
        bubbleView = inflater.inflate(R.layout.layout_floating_bubble, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        setupBubbleTouchListener()
        try {
            windowManager.addView(bubbleView, bubbleParams)
            updateBubbleStatus(config.isActive)
        } catch (e: Exception) {
            Log.e("GlitchOverlay", "Error adding bubbleView to WindowManager: ${e.message}", e)
        }
    }

    private fun setupBubbleTouchListener() {
        val bubble = bubbleView ?: return
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        bubble.setOnTouchListener { _, event ->
            val params = bubbleParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)
                    if (deltaX < 10 && deltaY < 10) {
                        // Click detected! Toggle floating window HUD
                        toggleFloatingWindow()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleFloatingWindow() {
        if (isWindowExpanded) {
            dismissFloatingWindow()
        } else {
            showFloatingWindow()
        }
    }

    private fun showFloatingWindow() {
        if (windowView != null) return

        val inflater = LayoutInflater.from(this)
        windowView = inflater.inflate(R.layout.layout_floating_window, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        setupWindowControls(windowView!!)
        try {
            windowManager.addView(windowView, windowParams)
            isWindowExpanded = true
        } catch (e: Exception) {
            Log.e("GlitchOverlay", "Error adding windowView to WindowManager: ${e.message}", e)
        }
    }

    private fun dismissFloatingWindow() {
        windowView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e("GlitchOverlay", "Error removing windowView: ${e.message}")
            }
            windowView = null
        }
        isWindowExpanded = false
    }

    private fun setupWindowControls(view: View) {
        val header = view.findViewById<View>(R.id.header_bar)
        val btnMinimize = view.findViewById<TextView>(R.id.btn_minimize)
        val btnClose = view.findViewById<TextView>(R.id.btn_close)
        val btnToggle = view.findViewById<Button>(R.id.btn_toggle_active)
        val etTarget = view.findViewById<EditText>(R.id.et_target_filter)
        val cbTurbo = view.findViewById<CheckBox>(R.id.cb_turbo_spam)
        val btnForceRoast = view.findViewById<Button>(R.id.btn_instant_roast)
        val btnReset = view.findViewById<Button>(R.id.btn_clear_counter)

        // Setup Header drag
        setupHeaderDrag(header)

        btnMinimize.setOnClickListener {
            dismissFloatingWindow()
        }

        btnClose.setOnClickListener {
            dismissFloatingWindow()
            stopSelf()
        }

        // Initialize target filter
        etTarget.setText(config.targetFilter)
        etTarget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                config.targetFilter = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Initialize Turbo Spam Checkbox
        cbTurbo.isChecked = config.turboSpam
        cbTurbo.setOnCheckedChangeListener { _, isChecked ->
            config.turboSpam = isChecked
        }

        // Force Roast button
        btnForceRoast.setOnClickListener {
            GlitchAccessibilityService.instance?.forceRoastNow()
        }

        // Reset counter
        btnReset.setOnClickListener {
            config.resetSentCount()
            updateHudStats("Counter Reset", 0)
        }

        // Master Toggle Button
        updateWindowButtons()
        btnToggle.setOnClickListener {
            config.isActive = !config.isActive
            updateBubbleStatus(config.isActive)
            updateWindowButtons()
        }
    }

    private fun setupHeaderDrag(header: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        header.setOnTouchListener { _, event ->
            val wView = windowView ?: return@setOnTouchListener false
            val params = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(wView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateWindowButtons() {
        val view = windowView ?: return
        val btnToggle = view.findViewById<Button>(R.id.btn_toggle_active)
        val tvStatus = view.findViewById<TextView>(R.id.tv_hud_status)

        if (config.isActive) {
            btnToggle.text = "🛑 STOP SPAMMER (ACTIVE)"
            btnToggle.setBackgroundResource(R.drawable.bg_neon_button)
            tvStatus.text = "Status: RUNNING • Auto-Hook Active"
            tvStatus.setTextColor(Color.parseColor("#00E676"))
        } else {
            btnToggle.text = "⚡ START AUTO-ROAST & SPAM"
            btnToggle.setBackgroundResource(R.drawable.bg_neon_button_cyan)
            tvStatus.text = "Status: PAUSED"
            tvStatus.setTextColor(Color.parseColor("#9CA3AF"))
        }
    }

    private fun updateBubbleStatus(isActive: Boolean) {
        val bubble = bubbleView ?: return
        val statusDot = bubble.findViewById<View>(R.id.view_status_dot)
        if (isActive) {
            statusDot?.setBackgroundColor(Color.parseColor("#00E676")) // Neon Green
        } else {
            statusDot?.setBackgroundColor(Color.parseColor("#FF0055")) // Red
        }
    }

    private fun updateHudStats(status: String, sentCount: Int) {
        val view = windowView ?: return
        val tvStatus = view.findViewById<TextView>(R.id.tv_hud_status)
        val tvStats = view.findViewById<TextView>(R.id.tv_hud_stats)

        tvStatus?.text = "Status: $status"
        tvStats?.text = "Sent: $sentCount | Speed: ${config.spamDelayMs}ms"
    }

    override fun onDestroy() {
        super.onDestroy()
        isOverlayRunning = false
        shakeDetector?.stop()
        dismissFloatingWindow()
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e("GlitchOverlay", "Error removing bubbleView: ${e.message}")
            }
            bubbleView = null
        }
    }
}
