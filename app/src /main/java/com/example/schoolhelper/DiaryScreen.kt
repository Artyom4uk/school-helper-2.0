package com.example.schoolhelper

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class DiaryScreen(activity: Activity) {

    private val context = activity
    private val homework = Store.loadHomework()
    private val grades = Store.loadGrades()
    private val schedule = Store.loadSchedule()
    private val days = listOf("Понедельник", "Вторник", "Среда", "Четверг",
        "Пятница", "Суббота", "Воскресенье")

    private var dueDate = ""
    private lateinit var hwAdapter: ArrayAdapter<String>
    private lateinit var gradeAdapter: ArrayAdapter<String>

    private val etHwSubject = activity.findViewById<AutoCompleteTextView>(R.id.etHwSubject)
    private val etHwTask = activity.findViewById<EditText>(R.id.etHwTask)
    private val btnHwDate = activity.findViewById<Button>(R.id.btnHwDate)
    private val btnHwAdd = activity.findViewById<Button>(R.id.btnHwAdd)
    private val llHomework = activity.findViewById<LinearLayout>(R.id.llHomework)

    private val etGradeSubject = activity.findViewById<AutoCompleteTextView>(R.id.etGradeSubject)
    private val tvAverage = activity.findViewById<TextView>(R.id.tvAverage)
    private val llGrades = activity.findViewById<LinearLayout>(R.id.llGrades)

    private val spDay = activity.findViewById<Spinner>(R.id.spDay)
    private val etLesson = activity.findViewById<EditText>(R.id.etLesson)
    private val btnLessonAdd = activity.findViewById<Button>(R.id.btnLessonAdd)
    private val llSchedule = activity.findViewById<LinearLayout>(R.id.llSchedule)

    init {
        // --- Домашка ---
        hwAdapter = makeSubjectAdapter()
        etHwSubject.setAdapter(hwAdapter)
        etHwSubject.threshold = 1
        btnHwDate.setOnClickListener { pickDate() }
        btnHwAdd.setOnClickListener { addHomework() }
        renderHomework()

        // --- Оценки ---
        gradeAdapter = makeSubjectAdapter()
        etGradeSubject.setAdapter(gradeAdapter)
        etGradeSubject.threshold = 1
        for (pair in listOf(R.id.btnG2 to 2, R.id.btnG3 to 3, R.id.btnG4 to 4, R.id.btnG5 to 5)) {
            activity.findViewById<Button>(pair.first).setOnClickListener { addGrade(pair.second) }
        }
        renderGrades()

        // --- Расписание ---
        spDay.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, days)
        spDay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                renderSchedule()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spDay.setSelection(todayIndex())
        btnLessonAdd.setOnClickListener { addLesson() }
        renderSchedule()
    }

    // ================= Домашка =================

    private fun addHomework() {
        val task = etHwTask.text.toString().trim()
        if (task.isEmpty()) { toast("Напиши, что задали"); return }
        val subject = etHwSubject.text.toString().trim().ifEmpty { "Без предмета" }
        homework.add(Homework(subject, task, dueDate, false))
        Store.saveHomework(homework)
        etHwTask.setText("")
        dueDate = ""
        btnHwDate.text = "Срок: без даты"
        renderHomework()
        refreshHints()
        toast("Добавлено ✅")
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            dueDate = String.format(Locale.US, "%02d.%02d.%d", day, month + 1, year)
            btnHwDate.text = "Срок: $dueDate"
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun renderHomework() {
        llHomework.removeAllViews()
        if (homework.isEmpty()) {
            llHomework.addView(note("Пока пусто — добавь первое задание"))
            return
        }
        for ((index, hw) in homework.withIndex()) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            val info = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val title = TextView(context).apply {
                text = "${hw.subject}: ${hw.task}"
                textSize = 16f
                if (hw.done) paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
            info.addView(title)
            if (hw.due.isNotEmpty()) {
                info.addView(TextView(context).apply {
                    text = "📅 до ${hw.due}"
                    textSize = 12f
                    setTextColor(GRAY)
                })
            }
            val check = CheckBox(context).apply {
                isChecked = hw.done
                setOnCheckedChangeListener { _, checked ->
                    hw.done = checked
                    title.paintFlags = if (checked) title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    else title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    Store.saveHomework(homework)
                }
            }
            row.addView(info)
            row.addView(check)
            row.addView(deleteButton {
                homework.removeAt(index)
                Store.saveHomework(homework)
                renderHomework()
                toast("Удалено")
            })
            llHomework.addView(row)
        }
    }

    // ================= Оценки =================

    private fun addGrade(value: Int) {
        val subject = etGradeSubject.text.toString().trim()
        if (subject.isEmpty()) { toast("Напиши предмет"); return }
        grades.add(Grade(subject, value))
        Store.saveGrades(grades)
        renderGrades()
        refreshHints()
    }

    private fun renderGrades() {
        llGrades.removeAllViews()
        tvAverage.text = if (grades.isEmpty()) "Средний балл: —"
        else "Средний балл: ${fmt(avg(grades.map { it.value }))} " +
                "(${grades.size} ${plural(grades.size, "оценка", "оценки", "оценок")})"

        if (grades.isEmpty()) {
            llGrades.addView(note("Добавь оценки кнопками 2–5"))
            return
        }
        val bySubject = linkedMapOf<String, MutableList<Int>>()
        for (g in grades) bySubject.getOrPut(g.subject) { mutableListOf() }.add(g.value)

        for ((subject, values) in bySubject) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            row.addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = "$subject: ${values.joinToString(" ")}  •  ср. ${fmt(avg(values))}"
                textSize = 15f
            })
            row.addView(deleteButton {
                AlertDialog.Builder(context)
                    .setTitle("Удалить оценки?")
                    .setMessage("Удалить все оценки по предмету «$subject»?")
                    .setPositiveButton("Удалить") { _, _ ->
                        grades.removeAll { it.subject == subject }
                        Store.saveGrades(grades)
                        renderGrades()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            })
            llGrades.addView(row)
        }
    }

    // ================= Расписание =================

    private fun addLesson() {
        val lesson = etLesson.text.toString().trim()
        if (lesson.isEmpty()) { toast("Введи название урока"); return }
        val day = spDay.selectedItemPosition
        schedule.getOrPut(day) { mutableListOf() }.add(lesson)
        Store.saveSchedule(schedule)
        etLesson.setText("")
        renderSchedule()
        refreshHints()
    }

    private fun renderSchedule() {
        llSchedule.removeAllViews()
        val day = spDay.selectedItemPosition
        if (day < 0) return
        val lessons = schedule[day].orEmpty()
        if (lessons.isEmpty()) {
            llSchedule.addView(note("Уроков нет — добавь первый"))
            return
        }
        lessons.forEachIndexed { i, lesson ->
            llSchedule.addView(TextView(context).apply {
                text = "${i + 1}. $lesson"
                textSize = 16f
                setPadding(0, dp(8), 0, dp(8))
                setOnClickListener {
                    lessons.removeAt(i)
                    if (lessons.isEmpty()) schedule.remove(day)
                    Store.saveSchedule(schedule)
                    renderSchedule()
                    toast("Удалено")
                }
            })
        }
        llSchedule.addView(note("Нажми на урок, чтобы удалить его"))
    }

    // ================= Вспомогательное =================

    private fun allSubjects(): List<String> =
        (homework.map { it.subject } + grades.map { it.subject } + schedule.values.flatten())
            .filter { it.isNotBlank() && it != "Без предмета" }
            .distinct()

    private fun makeSubjectAdapter() =
        ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, allSubjects().toMutableList())

    private fun refreshHints() {
        val subjects = allSubjects()
        hwAdapter.clear(); hwAdapter.addAll(subjects); hwAdapter.notifyDataSetChanged()
        gradeAdapter.clear(); gradeAdapter.addAll(subjects); gradeAdapter.notifyDataSetChanged()
    }

    private fun todayIndex(): Int = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

    private fun avg(values: List<Int>): Double =
        if (values.isEmpty()) 0.0 else values.sum().toDouble() / values.size

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)

    private fun note(text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 13f
        setTextColor(GRAY)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun deleteButton(onClick: () -> Unit): TextView = TextView(context).apply {
        text = "✕"
        textSize = 16f
        setTextColor(GRAY)
        setPadding(dp(12), dp(4), dp(4), dp(4))
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    private fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    companion object {
        private val GRAY = 0xFF8A8A8A.toInt()
    }
}
