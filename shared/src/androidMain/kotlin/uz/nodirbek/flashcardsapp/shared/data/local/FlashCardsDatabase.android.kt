package uz.nodirbek.flashcardsapp.shared.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<FlashCardsDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("flashcards_database")
    return Room.databaseBuilder<FlashCardsDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
