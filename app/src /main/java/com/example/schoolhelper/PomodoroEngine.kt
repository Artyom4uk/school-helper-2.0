package com.example.schoolhelper

import android.os.CountDownTimer

object PomodoroEngine {
    const val WORK_MINUTES = 25L
    const val BREAK_MINUTES = 5L

    var isWork = true
    var remainingMs = WORK_MINUTES * 60_000L
    var running = false
        private set

    private var timer: CountDownTimer? = null
    var onTick: ((Long) -> Unit)? = null
    var onFinished: ((Boolean) -> Unit)? = null   // true = закончилась учёба

    fun start() {
        if (running) return
        running = true
        timer = object : CountDownTimer(remainingMs, 250) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                onTick?.invoke(ms)
            }
            override fun onFinish() {
                running = false
                remainingMs = 0
                onFinished?.invoke(isWork)
            }
        }.start()
    }

    fun pause() {
        timer?.cancel()
        running = false
    }

    fun setMode(work: Boolean) {
        pause()
        isWork = work
        remainingMs = (if (work) WORK_MINUTES else BREAK_MINUTES) * 60_000L
        onTick?.invoke(remainingMs)
    }

    fun reset() = setMode(isWork)
}
