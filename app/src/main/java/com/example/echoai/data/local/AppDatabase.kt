package com.example.echoai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Session::class,
        AudioChunk::class,
        TranscriptLine::class,
        Summary::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptLineDao(): TranscriptLineDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE summaries ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE summaries ADD COLUMN actionItemsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE summaries ADD COLUMN keyPointsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE summaries ADD COLUMN status TEXT NOT NULL DEFAULT 'IDLE'")
                db.execSQL("ALTER TABLE summaries ADD COLUMN error TEXT")
            }
        }
    }
}
