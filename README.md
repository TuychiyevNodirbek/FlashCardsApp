# Тетрадь слов (Flash Cards App)

A modern Android flashcard learning app with spaced repetition algorithm (SM-2), built with Kotlin and Jetpack Compose.

## Features

- **Flashcard Management**: Add, edit, and manage flashcards
- **Spaced Repetition**: SM-2 algorithm for optimal learning intervals
- **CSV Import**: Import cards from CSV/TSV files with automatic delimiter detection
- **Gamification**: Track XP, levels, and daily streaks
- **Local Notifications**: Daily study reminders at a configurable time
- **Offline First**: All data stored locally, no network required
- **Material 3 UI**: Modern Material Design 3 with Jetpack Compose

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Database**: Room (SQLite)
- **Async**: Kotlin Coroutines + Flow
- **Preferences**: Jetpack DataStore
- **Notifications**: AlarmManager + BroadcastReceiver
- **Navigation**: Navigation Compose

## Building the App

### Requirements
- Android Studio Jellyfish or later
- Android SDK 26+ (API level 26)
- JDK 11 or later

### Build Steps

```bash
# Clone the repository
cd FlashCardsApp

# Build the debug APK
./gradlew assembleDebug

# Build the release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run on device/emulator
./gradlew installDebug
```

### Build Outputs
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
app/src/main/java/uz/nodirbek/flashcardsapp/
├── data/
│   ├── local/
│   │   ├── database/        # Room database
│   │   └── preferences/     # DataStore preferences
│   └── repository/          # Data access layer
├── domain/
│   ├── model/               # Domain models
│   └── usecase/             # Business logic (SM-2 algorithm)
├── notification/            # Notification and alarm handling
├── ui/
│   ├── navigation/          # Navigation graph
│   ├── screen/              # Compose screens
│   ├── state/               # UI state models
│   ├── viewmodel/           # ViewModels
│   └── theme/               # Material 3 theme
└── MainActivity.kt
```

## Key Features Implementation

### Spaced Repetition (SM-2)
The core algorithm implements a simplified SM-2 with:
- 4 quality levels: Again (0), Hard (1), Good (2), Easy (3)
- Dynamic ease factor adjustment
- Configurable intervals

Test the algorithm: `RateCardUseCaseTest`

### CSV Import
- Auto-detects delimiter (Tab, Semicolon, Comma)
- Handles up to 5000 cards per import
- Validates card pairs before import

### Notifications
- Daily reminder at configured time
- Works even when app is closed (AlarmManager + BroadcastReceiver)
- Deep link to study screen

### Gamification
- **Level**: Calculated as `floor(XP / 100) + 1`
- **XP**: +10 for Hard/Good/Easy, +4 for Again
- **Streak**: Increments on consecutive active days

## Testing

### Unit Tests
```bash
./gradlew test
```

Key tests:
- SM-2 algorithm progression and recovery
- Edge cases and boundary conditions
- Ease factor adjustments

### Manual Testing Checklist

- [ ] App works in airplane mode (no network calls)
- [ ] Notifications work after app restart
- [ ] CSV import from sample file works
- [ ] Cards display correctly with 5000+ items (scroll performance)
- [ ] Colors use MaterialTheme tokens (check dark mode works)

## Acceptance Criteria

✅ Works completely offline (airplane mode)
✅ Notifications arrive at scheduled time
✅ CSV import handles test files without errors
✅ MVVM architecture with clean separation
✅ Unit tests for SM-2 algorithm
✅ Material 3 theming (no hardcoded colors)

## Permissions

The app requests:
- `POST_NOTIFICATIONS` (Android 13+) - for study reminders
- `SCHEDULE_EXACT_ALARM` - for notification scheduling
- `READ_EXTERNAL_STORAGE` - for CSV file import

## Database Schema

### Cards Table
```
id (TEXT, PK)           - UUID
front (TEXT)            - Card front text
back (TEXT)             - Card back text
ease (REAL)             - Ease factor (default 2.5)
reps (INT)              - Repetitions count
interval (INT)          - Days until next review
dueDate (TEXT)          - ISO date string
lastReviewed (TEXT?)    - Last review date
createdAt (LONG)        - Creation timestamp
```

### User Stats (DataStore)
```
streak (INT)            - Current streak
lastActiveDate (TEXT?)  - Last activity date
xp (LONG)               - Total XP
reminderEnabled (BOOL)  - Notification toggle
reminderTime (TEXT)     - HH:mm format
```

## License

MIT License
