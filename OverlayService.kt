package com.example.chessoverlay

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    // Стартовая позиция в FEN-подобном виде, построчно сверху (8-я горизонталь) вниз (1-я)
    private val startPosition = arrayOf(
        "♜♞♝♛♚♝♞♜",
        "♟♟♟♟♟♟♟♟",
        "        ",
        "        ",
        "        ",
        "        ",
        "♙♙♙♙♙♙♙♙",
        "♖♘♗♕♔♗♘♖"
    )

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        showOverlay()
    }

    private fun startForegroundWithNotification() {
        val channelId = "chess_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Chess Overlay", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Chess Overlay активен")
            .setSmallIcon(android.R.drawable.ic_menu_help)
            .build()
        startForeground(1, notification)
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val inflater = android.view.LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_chess, null)
        overlayView = view

        val grid = view.findViewById<GridLayout>(R.id.chessGrid)
        val squareSizePx = 42
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val square = TextView(this)
                val piece = startPosition[row][col]
                square.text = if (piece == ' ') "" else piece.toString()
                square.textSize = 22f
                square.gravity = Gravity.CENTER
                square.setTextColor(Color.BLACK)
                val isLight = (row + col) % 2 == 0
                square.setBackgroundColor(if (isLight) Color.parseColor("#EEEED2") else Color.parseColor("#769656"))

                val params = GridLayout.LayoutParams()
                params.width = squareSizePx
                params.height = squareSizePx
                params.rowSpec = GridLayout.spec(row)
                params.columnSpec = GridLayout.spec(col)
                square.layoutParams = params
                grid.addView(square)
            }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        // Перетаскивание окна пальцем
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        view.findViewById<TextView>(R.id.btnCloseOverlay).setOnClickListener {
            stopSelf()
        }

        windowManager.addView(view, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
