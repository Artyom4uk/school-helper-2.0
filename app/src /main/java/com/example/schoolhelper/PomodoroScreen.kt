package com.example.schoolhelper

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class PomodoroScreen(activity: Activity) {

    private val context = activity
    private val tvMode = activity.findViewById<TextView>(R.id.tvPomoMode)
    private val tvTime = activity.findViewById<TextView>(R.id.tvPomoTime)
    private val tvCount = activity.findViewById<TextView>(R.id.tvPomoCount)
    private val btnWork = activity.findViewById<Button>(R.id.btnPomoWork)
    private val btnBreak = activity.findViewById<Button>(R.id.btnPomoBreak)
    private val btnStart = activity.findViewById<Button>(R.id.btnPomoStart)
    private val btnReset = activity.findViewById<Button>(R.id.btnPomoReset)

    private val tone: ToneGenerator? =
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }.getOrNull()

    init {
        tvCount.text = "🍅 Помидорок сегодня: ${Store.loadPomodoros()}"

        btnWork.setOnClickListener { setMode(true) }
        btnBreak.setOnClickListener { setMode(false) }
        btnStart.setOnClickListener {
            if (PomodoroEngine.running) PomodoroEngine.pause() else PomodoroEngine.start()
            updateStartButton()
        }
        btnReset.setOnClickListener {
            PomodoroEngine.reset()
            updateStartButton()
        }

        PomodoroEngine.onTick = { ms -> tvTime.text = format(ms) }
        PomodoroEngine.onFinished = { wasWork -> onFinished(wasWork) }

        tvTime.text = format(PomodoroEngine.remainingMs)
        highlightMode()
        updateStartButton()
    }

    private fun setMode(work: Boolean) {
        PomodoroEngine.setMode(work)
        highlightMode()
        updateStartButton()
    }

    private fun highlightMode() {
        tvMode.text = if (PomodoroEngine.isWork) "Время учиться 💪" else "Перерыв — отдыхай ☕"
        btnWork.alpha = if (PomodoroEngine.isWork) 1f else 0.45f
        btnBreak.alpha = if (PomodoroEngine.isWork) 0.45f else 1f
    }

    private fun updateStartButton() {
        btnStart.text = if (PomodoroEngine.running) "Пауза" else "Старт"
    }

    private fun onFinished(wasWork: Boolean) {
        alarm()
        if (wasWork) {
            val count = Store.loadPomodoros() + 1
            Store.savePomodoros(count)
            tvCount.text = "🍅 Помидорок сегодня: $count"
            Toast.makeText(context, "25 минут сделано! Перерыв 🎉", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Перерыв закончен — за работу!", Toast.LENGTH_LONG).show()
        }
        PomodoroEngine.setMode(!wasWork)
        highlightMode()
        updateStartButton()
    }

    private fun alarm() {
        runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 1500) }
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(600)
    }

    private fun format(ms: Long): String {
        val seconds = (ms + 999) / 1000
        return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
    }
}
