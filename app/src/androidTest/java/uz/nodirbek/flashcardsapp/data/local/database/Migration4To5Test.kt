package uz.nodirbek.flashcardsapp.data.local.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test-db"

/**
 * Схема v4 никогда не экспортировалась, поэтому вместо MigrationTestHelper
 * создаём базу v4 вручную и прогоняем MIGRATION_4_5 напрямую.
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun deleteDb() {
        context.deleteDatabase(TEST_DB)
    }

    private fun createV4Database(): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE cards (
                            id TEXT NOT NULL PRIMARY KEY,
                            deckId TEXT NOT NULL DEFAULT 'default',
                            front TEXT NOT NULL,
                            back TEXT NOT NULL,
                            ease REAL NOT NULL,
                            reps INTEGER NOT NULL,
                            interval INTEGER NOT NULL,
                            dueDate TEXT NOT NULL,
                            lastReviewed TEXT,
                            createdAt INTEGER NOT NULL
                        )
                        """
                    )
                    db.execSQL(
                        """
                        CREATE TABLE decks (
                            id TEXT NOT NULL PRIMARY KEY,
                            name TEXT NOT NULL,
                            parentId TEXT,
                            colorHex TEXT NOT NULL DEFAULT '#4255FF',
                            createdAt INTEGER NOT NULL,
                            isPinned INTEGER NOT NULL DEFAULT 0,
                            pinnedAt INTEGER NOT NULL DEFAULT 0
                        )
                        """
                    )
                    db.execSQL(
                        "INSERT INTO decks (id, name, parentId, colorHex, createdAt) " +
                            "VALUES ('d1', 'Deck', NULL, '#4255FF', 1000)"
                    )
                    db.execSQL(
                        "INSERT INTO cards (id, deckId, front, back, ease, reps, interval, dueDate, createdAt) " +
                            "VALUES ('c1', 'd1', 'hello', 'привет', 2.5, 0, 0, '2026-01-01', 2000)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun migrate4To5_addsColumnsBackfillsAndCreatesIndexes() {
        val db = createV4Database()

        FlashCardsDatabase.MIGRATION_4_5.migrate(db)

        // Новые колонки + backfill updatedAt = createdAt
        db.query("SELECT sortOrder, updatedAt FROM decks WHERE id = 'd1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
            assertEquals(1000L, c.getLong(1))
        }
        db.query("SELECT updatedAt FROM cards WHERE id = 'c1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2000L, c.getLong(0))
        }

        // Данные не потеряны
        db.query("SELECT front, back FROM cards WHERE id = 'c1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("hello", c.getString(0))
            assertEquals("привет", c.getString(1))
        }

        // Индексы созданы
        val indexNames = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'index'").use { c ->
            while (c.moveToNext()) indexNames.add(c.getString(0))
        }
        assertTrue("missing cards index in $indexNames", "index_cards_deckId" in indexNames)
        assertTrue("missing decks index in $indexNames", "index_decks_parentId" in indexNames)

        db.close()
    }
}
