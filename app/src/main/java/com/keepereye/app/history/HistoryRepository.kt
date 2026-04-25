package com.keepereye.app.history

import android.content.Context

class HistoryRepository(context: Context) {

    private val dbHelper = HistoryDatabaseHelper(context)
    private var lastSavedText = ""

    fun saveText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || trimmed == lastSavedText || trimmed.length < 4) return
        lastSavedText = trimmed
        dbHelper.insertEntry(trimmed, System.currentTimeMillis())
    }

    fun getHistory(): List<HistoryEntry> {
        return dbHelper.getAllEntries()
    }

    fun clearHistory() {
        dbHelper.clearAll()
        lastSavedText = ""
    }
}
