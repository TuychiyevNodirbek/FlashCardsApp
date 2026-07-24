# ТЗ: Unit Flow — юниты по 10 слов с цепочкой упражнений (стиль Duolingo)

Это самодостаточное техническое задание. Выполнять фазы строго по порядку.
После каждой фазы проект должен компилироваться (`gradlew assembleDebug`) и работать.

## Контекст проекта

- Приложение: Kotlin + Jetpack Compose, flashcard-приложение со spaced repetition (SM-2).
- Пакет: `uz.nodirbek.flashcardsapp`.
- БД: Room, класс `data/local/database/FlashCardsDatabase.kt`, текущая версия **2**.
- Навигация: `ui/navigation/Screen.kt` (sealed class с route-строками) + `ui/navigation/NavGraph.kt`.
- Главный ViewModel: `ui/viewmodel/HomeViewModel.kt` (создаётся через `HomeViewModelFactory`).
- Существующие экраны упражнений: `ui/screen/FlashcardsScreen.kt`, `ui/screen/MatchScreen.kt`, `ui/screen/TestScreen.kt`, `ui/screen/StudyScreen.kt` (SM-2, в flow НЕ входит).
- TTS: `tts/TtsManager.kt` — уже умеет озвучивать слово.
- Модель карточки: `domain/model/Card.kt` (id: String, deckId, front, back, ease, reps, interval, dueDate, lastReviewed, createdAt).
- Дизайн: цвета `FdPrimary`, `FdGreen`, `FdRed` и т.п. в `ui/theme/Color.kt`, шрифт `OutfitFamily`, кнопки — `ui/components/PressButton.kt`, апбар — `ui/components/UnifiedAppBar.kt`. Поддерживается тёмная тема через `LocalIsDarkTheme` и `MaterialTheme.colorScheme`.

## Общая идея фичи

Слова колоды делятся на **юниты по 10 слов** (виртуально: сортировка по `createdAt`, срез по 10; отдельная таблица слов юнита НЕ нужна). Пользователь открывает юнит и проходит цепочку коротких упражнений на этих 10 словах:

`FLASHCARDS → MATCH → TEST → AUDIO → SCRAMBLE → WRITE`

Все шаги живут ВНУТРИ одного экрана `UnitFlowScreen` (переключение через `AnimatedContent`), а НЕ через отдельные navigation-маршруты. Наверху общий прогресс-бар. Прогресс юнита сохраняется в Room. Следующий юнит разблокируется после завершения предыдущего.

---

# ФАЗА 1 — данные, навигация, каркас flow (Flashcards → Match → Test)

## Шаг 1.1. Room: таблица прогресса юнитов

**Создать файл** `data/local/database/UnitProgressEntity.kt`:

```kotlin
package uz.nodirbek.flashcardsapp.data.local.database

import androidx.room.Entity

@Entity(tableName = "unit_progress", primaryKeys = ["deckId", "unitIndex"])
data class UnitProgressEntity(
    val deckId: String,
    val unitIndex: Int,          // 0-based номер юнита в колоде
    val completedSteps: Int = 0, // сколько шагов flow завершено (0..6)
    val completed: Boolean = false,
    val bestAccuracy: Float = 0f // лучшая точность за прохождение, 0..1
)
```

**Создать файл** `data/local/database/UnitProgressDao.kt`:

```kotlin
package uz.nodirbek.flashcardsapp.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitProgressDao {
    @Query("SELECT * FROM unit_progress WHERE deckId = :deckId")
    fun getForDeck(deckId: String): Flow<List<UnitProgressEntity>>

    @Query("SELECT * FROM unit_progress WHERE deckId = :deckId AND unitIndex = :unitIndex")
    suspend fun get(deckId: String, unitIndex: Int): UnitProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UnitProgressEntity)
}
```

**Изменить** `FlashCardsDatabase.kt`:
1. В `@Database(entities = [...])` добавить `UnitProgressEntity::class`.
2. Поднять `version = 2` до `version = 3`.
3. Добавить `abstract fun unitProgressDao(): UnitProgressDao`.
4. Добавить миграцию и зарегистрировать её в `addMigrations(MIGRATION_1_2, MIGRATION_2_3)`:

```kotlin
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
```

## Шаг 1.2. Доменная модель и репозиторий юнитов

**Создать файл** `domain/model/StudyUnit.kt`:

```kotlin
package uz.nodirbek.flashcardsapp.domain.model

data class StudyUnit(
    val deckId: String,
    val index: Int,            // 0-based
    val cards: List<Card>,     // 1..10 карточек
    val completedSteps: Int,
    val completed: Boolean,
    val bestAccuracy: Float,
    val locked: Boolean        // true, если предыдущий юнит не завершён
)
```

**Создать файл** `data/repository/UnitRepository.kt`. Смотри, как устроены `CardRepository` и `DeckRepository`, и сделай в том же стиле (те же конструкторные параметры-DAO). Логика:

```kotlin
package uz.nodirbek.flashcardsapp.data.repository

import uz.nodirbek.flashcardsapp.data.local.database.CardDao
import uz.nodirbek.flashcardsapp.data.local.database.UnitProgressDao
import uz.nodirbek.flashcardsapp.data.local.database.UnitProgressEntity
import uz.nodirbek.flashcardsapp.domain.model.StudyUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class UnitRepository(
    private val cardDao: CardDao,
    private val unitProgressDao: UnitProgressDao
) {
    companion object { const val UNIT_SIZE = 10 }

    // Юниты колоды: карточки сортируем по createdAt ASC (при равенстве — по id),
    // режем chunked(UNIT_SIZE). Последний юнит может быть короче 10 — это норма,
    // но юниты из МЕНЕЕ ЧЕМ 4 слов не создаём (хвост <4 слов приклеивается к предыдущему юниту;
    // если это единственный юнит — оставить как есть).
    fun getUnits(deckId: String): Flow<List<StudyUnit>> =
        combine(
            cardDao.getCardsByDeck(deckId),      // если метода с таким именем нет — найди в CardDao аналог и используй его
            unitProgressDao.getForDeck(deckId)
        ) { cards, progressList ->
            val sorted = cards.sortedWith(compareBy({ it.createdAt }, { it.id }))
            val chunks = buildChunks(sorted)      // реализация правила про хвост <4
            chunks.mapIndexed { i, chunk ->
                val p = progressList.find { it.unitIndex == i }
                val prevCompleted = i == 0 || progressList.find { it.unitIndex == i - 1 }?.completed == true
                StudyUnit(
                    deckId = deckId,
                    index = i,
                    cards = chunk.map { it.toDomain() }, // используй существующий маппер Entity->Card из проекта
                    completedSteps = p?.completedSteps ?: 0,
                    completed = p?.completed ?: false,
                    bestAccuracy = p?.bestAccuracy ?: 0f,
                    locked = !prevCompleted
                )
            }
        }

    suspend fun saveProgress(deckId: String, unitIndex: Int, completedSteps: Int, completed: Boolean, accuracy: Float) {
        val existing = unitProgressDao.get(deckId, unitIndex)
        unitProgressDao.upsert(
            UnitProgressEntity(
                deckId = deckId,
                unitIndex = unitIndex,
                completedSteps = maxOf(completedSteps, existing?.completedSteps ?: 0),
                completed = completed || (existing?.completed ?: false),
                bestAccuracy = maxOf(accuracy, existing?.bestAccuracy ?: 0f)
            )
        )
    }
}
```

ВАЖНО: перед написанием открой `CardDao.kt`, `CardRepository.kt` и `Card.kt` и используй РЕАЛЬНЫЕ имена методов/мапперов проекта, а не выдуманные. Подключи `UnitRepository` там же, где создаются остальные репозитории (найди место создания `CardRepository` — обычно `HomeViewModelFactory` или Application-класс — и повтори паттерн).

## Шаг 1.3. Навигация

**В `Screen.kt` добавить:**

```kotlin
object UnitList : Screen("units/{deckId}") {
    fun createRoute(deckId: String) = "units/$deckId"
    const val ARG = "deckId"
}
object UnitFlow : Screen("unit-flow/{deckId}/{unitIndex}") {
    fun createRoute(deckId: String, unitIndex: Int) = "unit-flow/$deckId/$unitIndex"
    const val ARG_DECK = "deckId"
    const val ARG_UNIT = "unitIndex"
}
object UnitResult : Screen("unit-result")
```

**В `NavGraph.kt`** добавить три `composable(...)` по образцу существующих (`Flashcards`, `Match`): достань аргументы через `backStackEntry.arguments`, `unitIndex` — как `Int` (`navArgument(ARG_UNIT) { type = NavType.IntType }`). Используй те же transition-функции из `Transitions.kt`, что и у соседних study-экранов.

**Точка входа:** в `DeckScreen.kt` в блок кнопок режимов (`StudyModeBtn`) добавить кнопку «Юниты» (emoji 🎯), которая вызывает новый колбэк `onNavigateToUnits: (String) -> Unit`; пробросить его через `NavGraph` → `navController.navigate(Screen.UnitList.createRoute(deckId))`.

## Шаг 1.4. UnitFlowViewModel

**Создать файл** `ui/viewmodel/UnitFlowViewModel.kt`:

```kotlin
package uz.nodirbek.flashcardsapp.ui.viewmodel

enum class FlowStep { FLASHCARDS, MATCH, TEST, AUDIO, SCRAMBLE, WRITE }
// В Фазе 1 активны только FLASHCARDS, MATCH, TEST. Список активных шагов держать в
// val steps: List<FlowStep> — в Фазе 1 = listOf(FLASHCARDS, MATCH, TEST),
// в Фазе 2 добавить AUDIO и WRITE, в Фазе 3 — SCRAMBLE. Весь остальной код
// должен опираться на steps, а не на захардкоженные номера шагов.

data class UnitFlowUiState(
    val cards: List<Card> = emptyList(),
    val stepIndex: Int = 0,             // индекс в steps
    val totalSteps: Int = 3,
    val correctAnswers: Int = 0,        // по шагам с проверкой (TEST/AUDIO/SCRAMBLE/WRITE)
    val totalAnswers: Int = 0,
    val finished: Boolean = false
)
```

ViewModel (`UnitFlowViewModel(unitRepository, deckId, unitIndex)` + Factory по образцу `HomeViewModelFactory`):
- `init`: загрузить юнит из `unitRepository.getUnits(deckId)` (взять `first()` из Flow, найти по `unitIndex`), положить карточки в state. Если юнит `locked` — всё равно позволить (защита на уровне UI списка).
- `fun onStepFinished(correct: Int, total: Int)`: прибавить к счёту, если `stepIndex` последний → `finished = true`, посчитать `accuracy = correctAnswers/totalAnswers` (если totalAnswers==0 → 1f), вызвать `saveProgress(..., completed = true, accuracy)`; иначе `stepIndex++` и `saveProgress(completedSteps = stepIndex, completed = false, ...)`.
- Шаги без проверки (FLASHCARDS, MATCH) вызывают `onStepFinished(0, 0)`.

## Шаг 1.5. Рефакторинг экранов упражнений в переиспользуемые «Content»-composable

Это главная работа фазы. Для каждого из трёх экранов:

**Правило рефакторинга (одинаковое для всех):** сейчас экран сам берёт карточки из `HomeViewModel` по deckId. Нужно выделить чистый composable, который принимает ГОТОВЫЙ список карточек и колбэк завершения, и не знает ни про viewModel, ни про навигацию:

1. `FlashcardsScreen.kt` → добавить `@Composable fun FlashcardsContent(cards: List<Card>, onDone: () -> Unit, modifier: Modifier = Modifier)` — вся текущая вёрстка листания карточек переезжает сюда. Старый `FlashcardsScreen(deckId, viewModel, onBackClick)` остаётся и внутри просто достаёт карточки и вызывает `FlashcardsContent` (обратная совместимость standalone-режима). Добавить кнопку/условие «Готово» на последней карточке → `onDone()`.
2. `MatchScreen.kt` → `MatchContent(cards: List<Card>, onDone: () -> Unit, modifier)` по тому же правилу. Если в юните >6 пар на экран не влезает — Match внутри показывает раундами по 5 пар, `onDone` после последнего раунда (если текущий MatchScreen уже так умеет — не трогать логику, только вынести).
3. `TestScreen.kt` → `TestContent(cards: List<Card>, onDone: (correct: Int, total: Int) -> Unit, modifier)`. Вопрос: `front`, 4 варианта из `back` (правильный + 3 случайных дистрактора из ТОЙ ЖЕ колоды; если в юните <4 карточек — брать дистракторы из всей колоды или уменьшить число вариантов). Если в текущем TestScreen генерация вопросов сидит в HomeViewModel — продублировать генерацию локально в `TestContent` через `remember` (проще и безопаснее, чем перепахивать HomeViewModel).

НЕ ломать существующие маршруты: после рефакторинга старые экраны должны работать ровно как раньше. Проверить, что все прежние вызовы компилируются.

## Шаг 1.6. Экраны фичи

**Создать** `ui/screen/UnitListScreen.kt`:
- `UnitListScreen(deckId, unitRepository/viewModel, onBack, onOpenUnit: (Int) -> Unit)`.
- `LazyColumn` карточек юнитов: «Юнит N», 10 слов, статус: ✅ пройден (показать bestAccuracy в %), ▶ текущий (кнопка «Начать» через `PressButton`), 🔒 заблокирован (серый, клик не работает). Стиль — как карточки в `HomeScreen`/`DeckScreen`, обязательно `MaterialTheme.colorScheme` для тёмной темы, апбар — `UnifiedAppBar`.

**Создать** `ui/screen/UnitFlowScreen.kt`:
- Сверху: кнопка ✕ (выход с диалогом подтверждения «Прогресс шага будет потерян») + `LinearProgressIndicator` с прогрессом `stepIndex / totalSteps` (анимировать через `animateFloatAsState`).
- Тело: `AnimatedContent(targetState = uiState.stepIndex)` (transition — slide влево, как в Transitions.kt) → по `steps[stepIndex]` рисуем `FlashcardsContent` / `MatchContent` / `TestContent`, передавая `uiState.cards` (перемешивать порядок карточек на каждом шаге через `remember(stepIndex) { cards.shuffled() }`).
- Когда `uiState.finished == true` → `onNavigateToResult(correct, total)`.

**Создать** `ui/screen/UnitResultScreen.kt` — по образцу `TestResultsScreen.kt`/`MatchDoneScreen.kt`: большой % точности, «Юнит пройден!», кнопка «К юнитам» (`popBackStack` до `UnitList`) и «Следующий юнит» (если есть). Передачу результата сделать так же, как сейчас передаются данные в `TestResults`/`MatchDone` (посмотри в NavGraph.kt как — и повтори тот же механизм).

## Шаг 1.7. Критерии приёмки Фазы 1
- Проект собирается: `gradlew assembleDebug` без ошибок.
- Из DeckScreen открывается список юнитов; колода из 25 слов даёт 3 юнита (10/10/5).
- Юнит 2 заблокирован, пока не пройден юнит 1.
- Прохождение: Flashcards → Match → Test → экран результата; прогресс-бар растёт.
- После убийства приложения статус «пройден» сохраняется (Room).
- Старые режимы (Flashcards/Match/Test standalone) работают как раньше.
- Тёмная тема выглядит корректно на всех новых экранах.

---

# ФАЗА 2 — упражнения Audio и Write + очередь ошибок

## Шаг 2.1. Audio → перевод

**Создать** `ui/screen/exercise/AudioContent.kt`:
- Сигнатура: `AudioContent(cards: List<Card>, tts: TtsManager, onDone: (correct: Int, total: Int) -> Unit, modifier)`.
- Логика = копия `TestContent`, но вместо текста вопроса — большая круглая кнопка 🔊 (при появлении вопроса автоматически озвучить `front` через `TtsManager` — посмотри его API в `tts/TtsManager.kt` и вызывай так же, как это делает существующий voice replay; повторный тап — озвучить ещё раз). Варианты ответов — `back` (перевод), 4 штуки.
- Получение `TtsManager` в composable — тем же способом, каким его получает экран с voice replay (найди использование TtsManager в ui/screen и повтори).
- В `UnitFlowViewModel.steps` добавить `AUDIO` после `TEST`; в `UnitFlowScreen` добавить ветку.

## Шаг 2.2. Write (перевод → напечатай слово)

**Создать** `ui/screen/exercise/WriteContent.kt`:
- Сигнатура: `WriteContent(cards: List<Card>, onDone: (correct: Int, total: Int) -> Unit, modifier)`.
- На экране: перевод (`back`) крупно, `OutlinedTextField` + кнопка «Проверить» (и `KeyboardActions` onDone). Клавиатуру показывать сразу (`FocusRequester` + `LaunchedEffect`).
- Проверка ответа — функция в этом же файле:

```kotlin
fun isAnswerCorrect(input: String, expected: String): Boolean {
    val a = input.trim().lowercase()
    val b = expected.trim().lowercase()
    if (a == b) return true
    return levenshtein(a, b) <= 1 && b.length >= 4 // короткие слова — только точное совпадение
}

fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
    }
    return dp[a.length][b.length]
}
```

- После проверки: зелёная плашка «Верно!» / красная с правильным ответом (стиль — как фидбек в TestScreen), кнопка «Дальше». При «почти верно» (левенштейн==1) — жёлтая плашка «Верно, но проверь написание: <слово>», засчитывается как верно.
- В `steps` добавить `WRITE` последним.

## Шаг 2.3. Очередь ошибок внутри шага

В `TestContent`, `AudioContent`, `WriteContent` (общий паттерн, можно вынести helper):
- Держать `val queue = remember { mutableStateListOf(...cards перемешанные...) }`.
- При ошибке карточка добавляется В КОНЕЦ очереди (повторно). Одна и та же карточка может вернуться максимум 2 раза (иначе бесконечный цикл на невыученном слове) — вести счётчик повторов в `mutableStateMapOf<String, Int>` по card.id.
- В `correct/total` для onDone считать только ПЕРВУЮ попытку по каждой карточке (повторы не влияют на accuracy).
- Прогресс внутри шага («3/10») показывать по числу карточек, отвеченных верно хотя бы раз.

## Шаг 2.4. Критерии приёмки Фазы 2
- Flow = Flashcards → Match → Test → Audio → Write, totalSteps = 5.
- Audio реально озвучивает слово при появлении вопроса и по тапу.
- Write прощает 1 опечатку в словах от 4 букв, показывает правильный ответ при ошибке.
- Ошибочная карточка возвращается в конце шага, максимум 2 раза.
- Accuracy на экране результата считается только по первым попыткам.
- Старые режимы не сломаны, сборка проходит.

---

# ФАЗА 3 — Scramble, интеграция с SM-2 и статистикой, полировка

## Шаг 3.1. Scramble (собери слово из букв)

**Создать** `ui/screen/exercise/ScrambleContent.kt`:
- Сигнатура: `ScrambleContent(cards: List<Card>, onDone: (correct: Int, total: Int) -> Unit, modifier)`.
- Показ: перевод (`back`) сверху; слово `front` разбито на буквы + 3 случайные лишние буквы того же алфавита, всё перемешано (`(word.toList() + extraLetters).shuffled()`), отображается как `FlowRow` чипов-кнопок.
- Тап по букве → буква переезжает в строку ответа (использованный чип гаснет). Тап по букве в строке ответа → возвращается обратно. Кнопка «Проверить» активна, когда длина ответа == длине слова. Пробелы/дефисы в слове показывать в строке ответа сразу как фиксированные (их не нужно выбирать).
- Ошибка → красная подсветка, показать правильное слово, «Дальше»; карточка в очередь ошибок (тот же паттерн из Фазы 2.3).
- Вставить `SCRAMBLE` в `steps` между `AUDIO` и `WRITE`. totalSteps = 6.

## Шаг 3.2. Интеграция с SM-2 и статистикой

- При завершении юнита (`completed = true` в первый раз): для каждой карточки юнита с `reps == 0` установить начальный интервал, чтобы слова попали в очередь SrsReview: вызвать существующую логику оценки из `domain/usecase/RateCardUseCase.kt` (открой и посмотри её API) с оценкой «good» — НЕ писать свою реализацию SM-2. Сделать это в `UnitFlowViewModel` при финише.
- Статистика: при финише юнита записать в DailyStats ответы (reviewCount += totalAnswers, correctCount += correctAnswers) через существующий `StatsRepository` — посмотри, как его вызывают StudyScreen/TestScreen, и повтори тот же путь.

## Шаг 3.3. Полировка UI

- `UnitResultScreen`: если accuracy ≥ 0.9 — заголовок «Идеально! 🏆», конфетти-анимация (если в проекте есть готовая — использовать; если нет — простая анимация из 20 падающих цветных Box через `rememberInfiniteTransition`, не подключать новые библиотеки).
- `UnitListScreen`: у текущего юнита показать мини-прогресс «шаг X из 6», продолжение с сохранённого шага НЕ делаем (юнит всегда начинается с шага 0 — это осознанное упрощение; completedSteps используется только для отображения).
- Переходы между шагами в `AnimatedContent` — slide+fade, длительности взять из `Transitions.kt`, чтобы стиль совпадал с остальным приложением.
- Проверить обе темы (светлая/тёмная) на всех новых экранах: нигде не должно быть захардкоженного белого/чёрного фона — только `MaterialTheme.colorScheme`.

## Шаг 3.4. Критерии приёмки Фазы 3
- Полный flow из 6 шагов работает.
- После первого прохождения юнита его слова появляются в SrsReview (StudyScreen) как назначенные к повторению.
- Статистика на StatsScreen учитывает ответы из юнитов.
- Scramble корректно работает со словами с пробелом/дефисом и не даёт собрать слово неправильной длины.
- Сборка и обе темы — ок.

---

# Общие правила для исполнителя (ОБЯЗАТЕЛЬНО)

1. Перед изменением любого файла — открой и прочитай его целиком. Используй реальные имена методов проекта, не из этого ТЗ, если они расходятся.
2. Не менять поведение существующих экранов и маршрутов. Рефакторинг = вынос кода, не переписывание.
3. Не добавлять новые зависимости в gradle.
4. Все строки UI — на русском, в стиле существующих экранов.
5. После каждого шага — компиляция `gradlew assembleDebug`; не переходить к следующему шагу с красной сборкой.
6. Комментарии в коде — только там, где есть неочевидное правило (например, «хвост <4 слов приклеивается к предыдущему юниту»).
