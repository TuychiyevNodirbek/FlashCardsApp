package uz.nodirbek.flashcardsapp.shared.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [CardEntity::class, DeckEntity::class, DailyStatsEntity::class, UnitProgressEntity::class],
    version = 8,
    exportSchema = true
)
@ConstructedBy(FlashCardsDatabaseConstructor::class)
abstract class FlashCardsDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun deckDao(): DeckDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun unitProgressDao(): UnitProgressDao
}

/** Реализуется Room-компилятором на каждой платформе — тело генерируется автоматически. */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object FlashCardsDatabaseConstructor : RoomDatabaseConstructor<FlashCardsDatabase> {
    override fun initialize(): FlashCardsDatabase
}

// Платформенный билдер объявляется отдельно на каждой платформе (не через expect/actual,
// т.к. на Android он принимает Context, а на iOS — нет): см. androidMain/iosMain.

internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE cards ADD COLUMN stability REAL NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE cards ADD COLUMN difficulty REAL NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE decks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE decks ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE cards ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE cards ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE cards ADD COLUMN lapses INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE decks ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE decks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE cards ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE decks SET updatedAt = createdAt")
        connection.execSQL("UPDATE cards SET updatedAt = createdAt")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_cards_deckId ON cards(deckId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_decks_parentId ON decks(parentId)")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE decks ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE decks ADD COLUMN pinnedAt INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS unit_progress (
                deckId TEXT NOT NULL,
                unitIndex INTEGER NOT NULL,
                completedSteps INTEGER NOT NULL DEFAULT 0,
                completed INTEGER NOT NULL DEFAULT 0,
                bestAccuracy REAL NOT NULL DEFAULT 0,
                PRIMARY KEY (deckId, unitIndex)
            )
            """.trimIndent()
        )
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // Add deckId to existing cards (default deck)
        connection.execSQL("ALTER TABLE cards ADD COLUMN deckId TEXT NOT NULL DEFAULT 'default'")

        // Create decks table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS decks (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                parentId TEXT,
                colorHex TEXT NOT NULL DEFAULT '#4255FF',
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Insert default deck so existing cards have a valid home
        connection.execSQL(
            """
            INSERT OR IGNORE INTO decks (id, name, parentId, colorHex, createdAt)
            VALUES ('default', 'Мои карточки', NULL, '#4255FF', ${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()})
            """.trimIndent()
        )

        // Create daily_stats table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_stats (
                date TEXT NOT NULL PRIMARY KEY,
                reviewCount INTEGER NOT NULL DEFAULT 0,
                correctCount INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}

internal val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
)

fun buildDatabase(builder: RoomDatabase.Builder<FlashCardsDatabase>): FlashCardsDatabase =
    builder
        .addMigrations(*ALL_MIGRATIONS)
        .setDriver(BundledSQLiteDriver())
        // Dispatchers.IO не входит в общий (common) API kotlinx.coroutines — на Kotlin/Native
        // он internal. Default — единственный корректно мультиплатформенный вариант здесь.
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
