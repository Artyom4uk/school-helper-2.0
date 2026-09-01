package com.example.schoolhelper

import android.app.Activity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.util.Locale
import kotlin.math.ceil

class GradeCalcScreen(activity: Activity) {

    private val etSubject = activity.findViewById<EditText>(R.id.etCalcSubject)
    private val etManual = activity.findViewById<EditText>(R.id.etCalcManual)
    private val etTarget = activity.findViewById<EditText>(R.id.etCalcTarget)
    private val tvResult = activity.findViewById<TextView>(R.id.tvCalcResult)

    init {
        activity.findViewById<Button>(R.id.btnCalc).setOnClickListener { calculate() }
    }

    private fun calculate() {
        // Откуда берём оценки: вручную или из дневника по предмету
        val manual = etManual.text.toString().trim()
        val values: List<Int> = if (manual.isNotEmpty()) {
            manual.split(Regex("[\\s,;]+"))
                .mapNotNull { it.toIntOrNull() }
                .filter { it in 2..5 }
        } else {
            val subject = etSubject.text.toString().trim()
            if (subject.isEmpty()) {
                tvResult.text = "Введи оценки вручную или напиши предмет из дневника."
                return
            }
            Store.loadGrades()
                .filter { it.subject.equals(subject, ignoreCase = true) }
                .map { it.value }
        }
        if (values.isEmpty()) {
            tvResult.text = "Оценок не нашлось. Проверь название предмета или введи оценки сам."
            return
        }

        val target = etTarget.text.toString().replace(',', '.').toDoubleOrNull()
        if (target == null || target < 2.0 || target > 5.0) {
            tvResult.text = "Укажи желаемый средний балл — число от 2.0 до 5.0 (например, 4.5)."
            return
        }

        val count = values.size
        val sum = values.sum()
        val avg = sum.toDouble() / count

        val sb = StringBuilder()
        sb.append("Сейчас: $count ${plural(count, "оценка", "оценки", "оценок")}, средний ${fmt(avg)}\n\n")

        if (avg >= target - 1e-9) {
            sb.append("🎉 Поздравляю! Твой средний ${fmt(avg)} уже не ниже ${fmt(target)}.")
            tvResult.text = sb.toString()
            return
        }

        if (target >= 5.0) {
            sb.append("Средний ровно 5.0 — только если ВСЕ оценки пятёрки. Попробуй цель 4.8 или 4.9.")
            tvResult.text = sb.toString()
            return
        }

        // Сколько пятёрок подряд нужно: (sum + 5n) / (count + n) >= target
        val need = ceil((target * count - sum) / (5.0 - target) - 1e-9).toInt().coerceAtLeast(1)
        val futureAvg = (sum + 5.0 * need) / (count + need)

        sb.append("Чтобы средний стал не ниже ${fmt(target)}, нужно получить ещё $need " +
                "${plural(need, "пятёрку", "пятёрки", "пятёрок")} подряд (без оценок ниже).\n")
        sb.append("После этого средний будет ${fmt(futureAvg)}.\n\n")
        sb.append("А если следующая оценка будет:\n")
        for (grade in 5 downTo 2) {
            val next = (sum + grade) / (count + 1.0)
            val mark = if (next >= target - 1e-9) "  ✅" else ""
            sb.append(String.format(Locale.US, "• %d → средний %.2f%s\n", grade, next, mark))
        }
        tvResult.text = sb.toString()
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.2f", v)
}
