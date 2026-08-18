package com.callrecorder.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CallRecordingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callRecordingDao(): CallRecordingDao

    companion object {
        const val DATABASE_NAME = "call_recorder_db"
    }
}
