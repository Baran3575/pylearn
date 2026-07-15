package com.baran.pylearn

import android.content.Context
import org.json.JSONArray

data class Lesson(
    val id: String,
    val title: String,
    val subtitle: String,
    val explanation: String,
    val example: String,
    val exerciseText: String,
    val expected: String,
    val hint: String,
)

fun loadLessons(context: Context): List<Lesson> {
    val text = context.assets.open("lessons.json").bufferedReader().use { it.readText() }
    val arr = JSONArray(text)
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Lesson(
            id = o.getString("id"),
            title = o.getString("title"),
            subtitle = o.getString("subtitle"),
            explanation = o.getString("explanation"),
            example = o.getString("example"),
            exerciseText = o.getString("exerciseText"),
            expected = o.getString("expected"),
            hint = o.getString("hint"),
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
