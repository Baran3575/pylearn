package com.baran.pylearn

import android.content.Context

class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("pylearn", Context.MODE_PRIVATE)

    fun isDone(id: String): Boolean = prefs.getBoolean("done_$id", false)

    fun markDone(id: String) {
        if (!isDone(id)) {
            prefs.edit()
                .putBoolean("done_$id", true)
                .putInt("xp", xp() + XP_PER_LESSON)
                .apply()
        }
    }

    fun completedCount(ids: List<String>): Int = ids.count { isDone(it) }

    fun xp(): Int = prefs.getInt("xp", 0)

    companion object {
        const val XP_PER_LESSON = 10
    }
}
