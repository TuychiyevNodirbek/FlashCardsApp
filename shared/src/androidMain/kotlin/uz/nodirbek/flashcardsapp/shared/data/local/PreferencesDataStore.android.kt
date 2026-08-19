package uz.nodirbek.flashcardsapp.shared.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createDataStore(context: Context): DataStore<Preferences> =
    createPreferencesDataStore {
        context.applicationContext.filesDir.resolve("user_stats.preferences_pb").absolutePath
    }
