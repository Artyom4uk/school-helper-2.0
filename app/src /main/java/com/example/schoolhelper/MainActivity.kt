package com.example.schoolhelper

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContentView(R.layout.activity_main)

        // Каждый экран сам находит свои view и настраивает логику
        DiaryScreen(this)
        PomodoroScreen(this)
        GradeCalcScreen(this)

        val screens = mapOf(
            R.id.nav_diary to findViewById<View>(R.id.containerDiary),
            R.id.nav_pomodoro to findViewById<View>(R.id.containerPomodoro),
            R.id.nav_calc to findViewById<View>(R.id.containerCalc)
        )

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            for ((id, view) in screens) {
                view.visibility = if (id == item.itemId) View.VISIBLE else View.GONE
            }
            true
        }
        nav.selectedItemId = R.id.nav_diary
    }
}
