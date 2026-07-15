package com.baran.pylearn

import android.content.Context

class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("pylearn", Context.MODE_PRIVATE)

    fun isDone(id: String): Boolean = prefs.getBoolean("done_$id", false)

    fun markDone(id: String) {
        prefs.edit().putBoolean("done_$id", true).apply()
    }

    fun completedCount(ids: List<String>): Int = ids.count { isDone(it) }
}
