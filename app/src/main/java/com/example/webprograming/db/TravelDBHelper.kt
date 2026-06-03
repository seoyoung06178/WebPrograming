package com.example.webprograming.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.webprograming.model.TravelRecord

class TravelDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "travel.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "travel_records"
        const val COL_ID = "_id"
        const val COL_TITLE = "title"
        const val COL_VISIT_DATE = "visit_date"
        const val COL_MEMO = "memo"
        const val COL_PHOTO_PATH = "photo_path"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_VISIT_DATE TEXT NOT NULL,
                $COL_MEMO TEXT,
                $COL_PHOTO_PATH TEXT,
                $COL_LATITUDE REAL DEFAULT 0.0,
                $COL_LONGITUDE REAL DEFAULT 0.0,
                $COL_CREATED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertRecord(record: TravelRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, record.title)
            put(COL_VISIT_DATE, record.visitDate)
            put(COL_MEMO, record.memo)
            put(COL_PHOTO_PATH, record.photoPath)
            put(COL_LATITUDE, record.latitude)
            put(COL_LONGITUDE, record.longitude)
            put(COL_CREATED_AT, System.currentTimeMillis().toString())
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllRecords(orderBy: String = "$COL_VISIT_DATE DESC"): List<TravelRecord> {
        val records = mutableListOf<TravelRecord>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, orderBy)

        cursor.use {
            while (it.moveToNext()) {
                records.add(cursorToRecord(it))
            }
        }
        return records
    }

    fun getRecordById(id: Long): TravelRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, null,
            "$COL_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            return if (it.moveToFirst()) cursorToRecord(it) else null
        }
    }

    fun updateRecord(record: TravelRecord): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, record.title)
            put(COL_VISIT_DATE, record.visitDate)
            put(COL_MEMO, record.memo)
            put(COL_PHOTO_PATH, record.photoPath)
            put(COL_LATITUDE, record.latitude)
            put(COL_LONGITUDE, record.longitude)
        }
        return db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(record.id.toString()))
    }

    fun deleteRecord(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteAllRecords(): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, null, null)
    }

    private fun cursorToRecord(cursor: android.database.Cursor): TravelRecord {
        return TravelRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            visitDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_VISIT_DATE)),
            memo = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEMO)) ?: "",
            photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_PATH)) ?: "",
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE)),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_AT)) ?: ""
        )
    }
}
