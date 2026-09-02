package uz.nodirbek.flashcardsapp.data.transfer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.nodirbek.flashcardsapp.shared.data.transfer.AnkiImportException
import uz.nodirbek.flashcardsapp.shared.data.transfer.AnkiImportResult
import uz.nodirbek.flashcardsapp.shared.data.transfer.FdeckCard
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "AnkiApkgImporter"

/**
 * Импорт колод Anki (.apkg). Anki-пакет — это zip-архив с SQLite-базой
 * collection.anki21 (или collection.anki2 в старых версиях), где заметки (notes)
 * хранят поля в столбце `flds`, разделённые байтом 0x1F. Берём первые два поля
 * как front/back — этого достаточно для базовых колод (Basic, Basic-reversed и т.п.).
 * Медиа (картинки/аудио) намеренно не импортируются.
 */
class AnkiApkgImporter(private val context: Context) {

    private val fieldSeparator = '\u001F'

    suspend fun import(uri: Uri): AnkiImportResult = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "anki_import").apply { mkdirs() }
        val zipFile = File(cacheDir, "import_${System.currentTimeMillis()}.apkg")
        val dbFile = File(cacheDir, "import_${System.currentTimeMillis()}.sqlite")

        Log.d(TAG, "import() request: uri=$uri")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                zipFile.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                Log.e(TAG, "import() failed: contentResolver.openInputStream returned null for uri=$uri")
                throw AnkiImportException("Не удалось открыть файл")
            }
            Log.d(TAG, "import() response: copied ${zipFile.length()} bytes from uri=$uri to ${zipFile.path}")

            ZipFile(zipFile).use { zip ->
                Log.d(TAG, "import() zip entries: ${zip.entries().toList().map { it.name }}")
                val entry = zip.getEntry("collection.anki21")
                    ?: zip.getEntry("collection.anki2")
                    ?: if (zip.getEntry("collection.anki21b") != null) {
                        // Новый формат Anki (2.1.50+) хранит базу как zstd-сжатый collection.anki21b —
                        // это не обычный SQLite-файл, и без zstd-декомпрессии мы его прочитать не можем.
                        // Явно сообщаем об этом вместо ложного "это не файл Anki".
                        Log.e(TAG, "import() failed: only collection.anki21b (zstd) present, unsupported")
                        throw AnkiImportException(
                            "Колода экспортирована в новом формате Anki (zstd), который пока не поддерживается. " +
                                "Попробуйте экспортировать/скачать колоду в старом формате (Anki: File → Export → " +
                                "снять галочку \"Support older Anki versions\" оставить включённой)."
                        )
                    } else {
                        Log.e(TAG, "import() failed: no collection.anki21/anki2/anki21b entry found in zip")
                        throw AnkiImportException("Это не файл Anki (.apkg)")
                    }

                zip.getInputStream(entry).use { input ->
                    dbFile.outputStream().use { output -> input.copyTo(output) }
                }
                Log.d(TAG, "import() extracted ${entry.name} (${dbFile.length()} bytes) to ${dbFile.path}")
            }

            val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val (deckName, cards) = readDeckAndCards(db)
                Log.d(TAG, "import() response: deckName=\"$deckName\" cards=${cards.size}")
                if (cards.isEmpty()) throw AnkiImportException("В файле не найдено карточек")
                AnkiImportResult(deckName = deckName, cards = cards)
            } finally {
                db.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "import() exception for uri=$uri", e)
            throw e
        } finally {
            zipFile.delete()
            dbFile.delete()
        }
    }

    /**
     * .apkg может содержать несколько колод/подколод, экспортированных вместе (Anki при
     * экспорте колоды всегда включает все её подколоды). Раньше мы читали ВСЕ заметки из
     * всей базы разом, из-за чего в предпросмотре могло показаться больше карточек, чем
     * реально относится к нужной колоде. Теперь берём только ту колоду (did), в которой
     * реально больше всего карточек — это и есть содержательная колода, а не служебные
     * пустые/сопутствующие подколоды — и дополнительно убираем полные дубликаты пар.
     */
    private fun readDeckAndCards(db: SQLiteDatabase): Pair<String, List<FdeckCard>> {
        val bestDeck = findMostPopulatedDeck(db)
        val cursor = if (bestDeck != null) {
            db.rawQuery(
                "SELECT DISTINCT n.flds FROM notes n JOIN cards c ON c.nid = n.id WHERE c.did = ?",
                arrayOf(bestDeck.first)
            )
        } else {
            db.rawQuery("SELECT DISTINCT flds FROM notes", null)
        }
        val cards = mutableListOf<FdeckCard>()
        val seen = HashSet<String>()
        cursor.use {
            val fldsIndex = it.getColumnIndexOrThrow("flds")
            while (it.moveToNext()) {
                val flds = it.getString(fldsIndex) ?: continue
                val fields = flds.split(fieldSeparator)
                if (fields.size < 2) continue
                val front = stripHtml(fields[0])
                val back = stripHtml(fields[1])
                if (front.isNotBlank() && back.isNotBlank() && seen.add("$front $back")) {
                    cards.add(FdeckCard(front = front, back = back))
                }
            }
        }
        val deckName = bestDeck?.second?.takeIf { it.isNotBlank() } ?: "Anki импорт"
        return deckName to cards
    }

    /** Возвращает (did, имя) колоды с наибольшим числом карточек, либо null если распознать колоды не удалось. */
    private fun findMostPopulatedDeck(db: SQLiteDatabase): Pair<String, String>? {
        return try {
            val didToName = mutableMapOf<String, String>()
            db.rawQuery("SELECT decks FROM col LIMIT 1", null).use { cursor ->
                if (!cursor.moveToFirst()) return null
                val json = cursor.getString(0) ?: return null
                val obj = org.json.JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val name = obj.optJSONObject(id)?.optString("name").orEmpty()
                    if (name.isNotBlank()) didToName[id] = name
                }
            }
            if (didToName.isEmpty()) return null

            var bestDid: String? = null
            var bestCount = -1
            db.rawQuery("SELECT did, COUNT(*) FROM cards GROUP BY did", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val did = cursor.getString(0)
                    val count = cursor.getInt(1)
                    if (count > bestCount && didToName.containsKey(did)) {
                        bestCount = count
                        bestDid = did
                    }
                }
            }
            val did = bestDid ?: return null
            did to (didToName[did] ?: "Anki импорт")
        } catch (e: Exception) {
            null
        }
    }

    private val scriptStyleRegex = Regex("""<(script|style)[^>]*>.*?</\1>""", RegexOption.DOT_MATCHES_ALL)
    private val imgRegex = Regex("""<img\b[^>]*/?>""", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("""</?([a-zA-Z][a-zA-Z0-9]*)\b[^>]*>""")
    private val soundRegex = Regex("""\[sound:[^]]*]""")
    private val clozeRegex = Regex("""\{\{c\d+::(.*?)(::.*?)?\}\}""")
    private val whitespaceRegex = Regex("""[ \t]+""")

    /** Теги, форматирование которых сохраняем для отображения (см. HtmlText). Остальные вырезаются. */
    private val allowedTags = setOf("b", "strong", "i", "em", "u", "ul", "ol", "li", "br")

    private fun stripHtml(raw: String): String {
        var text = raw
            .replace(scriptStyleRegex, "")
            .replace(soundRegex, "")
            .replace(clozeRegex, "$1")
            .replace(imgRegex, "")
        text = tagRegex.replace(text) { match ->
            val tagName = match.groupValues[1].lowercase()
            if (tagName in allowedTags) {
                if (match.value.startsWith("</")) "</$tagName>" else "<$tagName>"
            } else {
                ""
            }
        }
        text = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
        text = whitespaceRegex.replace(text, " ")
        return text.trim()
    }
}
