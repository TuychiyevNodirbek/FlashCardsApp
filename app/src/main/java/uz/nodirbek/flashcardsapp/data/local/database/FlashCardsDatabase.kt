package uz.nodirbek.flashcardsapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CardEntity::class, DeckEntity::class, DailyStatsEntity::class, UnitProgressEntity::class],
    version = 3,
    exportSchema = false
)
abstract class FlashCardsDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun deckDao(): DeckDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun unitProgressDao(): UnitProgressDao

    companion object {
        @Volatile
        private var INSTANCE: FlashCardsDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS unit_progress (
                        deckId TEXT NOT NULL,
                        unitIndex INTEGER NOT NULL,
                        completedSteps INTEGER NOT NULL DEFAULT 0,
                        completed INTEGER NOT NULL DEFAULT 0,
                        bestAccuracy REAL NOT NULL DEFAULT 0,
                        PRIMARY KEY (deckId, unitIndex)
                    )
                """)
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add deckId to existing cards (default deck)
                db.execSQL("ALTER TABLE cards ADD COLUMN deckId TEXT NOT NULL DEFAULT 'default'")

                // Create decks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS decks (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        parentId TEXT,
                        colorHex TEXT NOT NULL DEFAULT '#4255FF',
                        createdAt INTEGER NOT NULL
                    )
                """)

                // Insert default deck so existing cards have a valid home
                db.execSQL("""
                    INSERT OR IGNORE INTO decks (id, name, parentId, colorHex, createdAt)
                    VALUES ('default', 'Мои карточки', NULL, '#4255FF', ${System.currentTimeMillis()})
                """)

                // Create daily_stats table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_stats (
                        date TEXT NOT NULL PRIMARY KEY,
                        reviewCount INTEGER NOT NULL DEFAULT 0,
                        correctCount INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        fun getDatabase(context: Context): FlashCardsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlashCardsDatabase::class.java,
                    "flashcards_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
