package com.example.gobang_pt

import android.content.Context

internal interface KidsProgressStore {
    fun completedLessonIds(): Set<String>

    fun markCompleted(lessonId: String)
}

internal class SharedPreferencesKidsProgress(context: Context) : KidsProgressStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun completedLessonIds(): Set<String> =
        preferences.getStringSet(KEY_COMPLETED_LESSONS, emptySet())?.toSet() ?: emptySet()

    override fun markCompleted(lessonId: String) {
        preferences.edit()
            .putStringSet(KEY_COMPLETED_LESSONS, completedLessonIds() + lessonId)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "kids_lessons"
        const val KEY_COMPLETED_LESSONS = "completed_lesson_ids"
    }
}

internal class InMemoryKidsProgress(initial: Set<String> = emptySet()) : KidsProgressStore {
    private val completed = initial.toMutableSet()

    override fun completedLessonIds(): Set<String> = completed.toSet()

    override fun markCompleted(lessonId: String) {
        completed.add(lessonId)
    }
}
