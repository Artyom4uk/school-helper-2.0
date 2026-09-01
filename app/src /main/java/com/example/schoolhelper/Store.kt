package com.example.schoolhelper

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Homework(val subject: String, val task: String, val due: String, var done: Boolean)
data class Grade(val subject: String, val value: Int)

object Store {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("school_helper", Context.MODE_PRIVATE)
    }

    // ---------- Домашние задания ----------
    fun loadHomework(): MutableList<Homework> {
        val list = mutableListOf<Homework>()
        runCatching {
            val arr = JSONArray(prefs.getString("homework", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(Homework(o.getString("subject"), o.getString("task"),
                    o.getString("due"), o.getBoolean("done")))
            }
        }
        return list
    }

    fun saveHomework(list: List<Homework>) {
        val arr = JSONArray()
        for (h in list) arr.put(
            JSONObject().put("subject", h.subject).put("task", h.task)
                .put("due", h.due).put("done", h.done)
        )
        prefs.edit().putString("homework", arr.toString()).apply()
    }

    // ---------- Оценки ----------
    fun loadGrades(): MutableList<Grade> {
        val list = mutableListOf<Grade>()
        runCatching {
            val arr = JSONArray(prefs.getString("grades", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(Grade(o.getString("subject"), o.getInt("value")))
            }
        }
        return list
    }

    fun saveGrades(list: List<Grade>) {
        val arr = JSONArray()
        for (g in list) arr.put(JSONObject().put("subject", g.subject).put("value", g.value))
        prefs.edit().putString("grades", arr.toString()).apply()
    }

    // ---------- Расписание: день (0 = понедельник) -> уроки ----------
    fun loadSchedule(): MutableMap<Int, MutableList<String>> {
        val map = linkedMapOf<Int, MutableList<String>>()
        runCatching {
            val o = JSONObject(prefs.getString("schedule", "{}"))
            for (key in o.keys()) {
                val arr = o.getJSONArray(key)
                val lessons = mutableListOf<String>()
                for (i in 0 until arr.length()) lessons.add(arr.getString(i))
                map[key.toInt()] = lessons
            }
        }
        return map
    }

    fun saveSchedule(map: Map<Int, List<String>>) {
        val o = JSONObject()
        for ((day, lessons) in map) o.put(day.toString(), JSONArray(lessons))
        prefs.edit().putString("schedule", o.toString()).apply()
    }

    // ---------- Помидорки (сбрасываются каждый день) ----------
    fun loadPomodoros(): Int {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        if (prefs.getString("pomodoro_date", "") != today) return 0
        return prefs.getInt("pomodoros", 0)
    }

    fun savePomodoros(count: Int) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        prefs.edit().putString("pomodoro_date", today).putInt("pomodoros", count).apply()
    }
}

// Русские множественные числа: 1 оценка / 2 оценки / 5 оценок
fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && (m100 < 12 || m100 > 14) -> few
        else -> many
    }
}
