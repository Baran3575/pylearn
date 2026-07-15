package com.baran.pylearn

import android.content.Context
import org.json.JSONArray

data class Step(
    val type: String,
    val text: String,
    val code: String,
    val expected: String,
    val hint: String,
) {
    val isTask: Boolean get() = type == "task"
}

data class Lesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val unit: String,
    val goal: String,
    val steps: List<Step>,
) {
    val taskCount: Int get() = steps.count { it.isTask }
}

fun loadLessons(context: Context): List<Lesson> {
    val text = context.assets.open("lessons.json").bufferedReader().use { it.readText() }
    val arr = JSONArray(text)
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val stepsArr = o.getJSONArray("steps")
        val steps = (0 until stepsArr.length()).map { j ->
            val s = stepsArr.getJSONObject(j)
            Step(
                type = s.getString("type"),
                text = s.getString("text"),
                code = s.optString("code"),
                expected = s.optString("expected"),
                hint = s.optString("hint"),
            )
        }
        Lesson(
            id = o.getString("id"),
            title = o.getString("title"),
            subtitle = o.getString("subtitle"),
            unit = o.optString("unit", "Bölüm"),
            goal = o.optString("goal", ""),
            steps = steps,
        )
    }
}

fun normalizeCode(s: String): String =
    s.replace('\'', '"')
        .lines()
        .map { it.trimEnd().replace(Regex(" +"), " ") }
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .trim()

fun checkAnswer(userCode: String, expected: String): Boolean =
    normalizeCode(userCode) == normalizeCode(expected)
