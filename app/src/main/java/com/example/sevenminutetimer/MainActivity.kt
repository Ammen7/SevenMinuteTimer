package com.example.sevenminutetimer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var countdown: TextView
    private val handler = android.os.Handler(mainLooper)
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        requestNotificationPermissionIfNeeded()
        handler.post(ticker)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
        }
        val title = TextView(this).apply {
            text = "Таймер каждые 7 минут"
            textSize = 26f
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            textSize = 19f
            gravity = Gravity.CENTER
            setPadding(0, 34, 0, 10)
        }
        countdown = TextView(this).apply {
            textSize = 42f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 30)
        }
        val start = Button(this).apply {
            text = "Старт"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(this@MainActivity, "Разреши точные будильники для точного интервала", Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")))
                        return@setOnClickListener
                    }
                }
                TimerCore.start(this@MainActivity)
                refresh()
            }
        }
        val stop = Button(this).apply {
            text = "Стоп"
            setOnClickListener {
                TimerCore.stop(this@MainActivity)
                refresh()
            }
        }
        root.addView(title)
        root.addView(status)
        root.addView(countdown)
        root.addView(start, LinearLayout.LayoutParams(-1, -2))
        root.addView(stop, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    private fun refresh() {
        if (!::status.isInitialized) return
        if (!TimerCore.isActive(this)) {
            status.text = "Таймер остановлен"
            countdown.text = "07:00"
            return
        }
        val left = (TimerCore.dueAt(this) - System.currentTimeMillis()).coerceAtLeast(0L)
        val minutes = (left / 60000).toInt()
        val seconds = ((left % 60000) / 1000).toInt()
        status.text = "Таймер работает · повтор каждые 7 минут"
        countdown.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
