package com.keepereye.app.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class HistoryDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TEXT TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    fun insertEntry(text: String, timestamp: Long): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TEXT, text)
            put(COL_TIMESTAMP, timestamp)
        }
        return db.insert(TABLE_HISTORY, null, values)
    }

    fun getAllEntries(): List<HistoryEntry> {
        val entries = mutableListOf<HistoryEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC",
            MAX_ENTRIES.toString()
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COL_ID))
                val text = it.getString(it.getColumnIndexOrThrow(COL_TEXT))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP))
                entries.add(HistoryEntry(id, text, timestamp))
            }
        }

        return entries
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_HISTORY, null, null)
    }

    fun getEntryCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_HISTORY", null)
        cursor.use {
            it.moveToFirst()
            return it.getInt(0)
        }
    }

    companion object {
        private const val DATABASE_NAME = "keeper_eye_history.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_HISTORY = "history"
        private const val COL_ID = "id"
        private const val COL_TEXT = "text"
        private const val COL_TIMESTAMP = "timestamp"
        private const val MAX_ENTRIES = "50"
    }
}
