# Сборка и запуск FlashCardsApp в Xcode (iOS)

> **Важно, прочитайте перед началом.** На iOS теперь работает **настоящая
> навигация**: онбординг → список колод → детали колоды → SRS-повторение,
> флеш-карточки, матч, тест, "грань забывания", прохождение юнита — все эти
> экраны уже общие (`commonMain`) и полностью функциональны на iOS. **Пока
> недоступны на iOS** (временные заглушки с кнопкой "Назад"): экран
> "Настройки", импорт колод (файловые пикеры), поиск по AnkiWeb (WebView) и
> Open Trivia DB (сетевой запрос) — они всё ещё используют Android-only API
> (`Context`/`WebView`/`HttpURLConnection`) и разбираются на общую+
> платформенную часть отдельно, в рамках Фазы 6. Уведомления
> (`notification/`-пакет) и сетевой слой `AnkiWebBrowser` тоже пока
> Android-only. Этот гайд даёт рабочий Xcode-проект, в котором реально можно
> дойти от онбординга до изучения карточек на iOS-симуляторе.

## 0. Что уже готово в репозитории (сделано на Windows, эта сессия)

Xcode-проект нельзя ни создать, ни собрать на Windows — эта часть гайда
описывает шаги, которые нужно проделать **на Mac**. Но KMP-сторона уже
подготовлена и проверена компиляцией (`:composeApp:compileKotlinIosSimulatorArm64`,
`BUILD SUCCESSFUL`):

- [`composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/App.kt`](../composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/App.kt) — корневой `@Composable fun App(container: AppContainer, ...)`: онбординг → тема → `NavGraph`. Вызывается и Android'ом (`MainActivity`), и iOS (`MainViewController`) — единая точка входа вместо раньше дублированной логики.
- [`composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/AppContainer.kt`](../composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/AppContainer.kt) — `expect class AppContainer` (DI-контейнер): Android-`actual` берёт `Context`, iOS-`actual` — нет (использует уже существующие iOS-actual для Room/DataStore из `:shared`).
- [`composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/ui/navigation/NavGraph.kt`](../composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/ui/navigation/NavGraph.kt) — перенесён из `androidMain` в `commonMain` целиком.
- [`composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/ui/navigation/PlatformScreens.kt`](../composeApp/src/commonMain/kotlin/uz/nodirbek/flashcardsapp/ui/navigation/PlatformScreens.kt) — `expect`-композаблы для 5 экранов, у которых пока нет общей реализации (Home/Settings/Import/AnkiWebBrowse/OpenTDBBrowse); iOS-`actual` для Home — настоящий работающий список колод, для остальных — заглушки.
- [`composeApp/src/iosMain/kotlin/uz/nodirbek/flashcardsapp/MainViewController.kt`](../composeApp/src/iosMain/kotlin/uz/nodirbek/flashcardsapp/MainViewController.kt) — `fun MainViewController(): UIViewController = ComposeUIViewController { App(container = remember { AppContainer() }) }`, точка входа, которую дальше зовёт Swift.
- В `composeApp/build.gradle.kts` — блок `binaries.framework { baseName = "ComposeApp"; isStatic = true }` для `iosArm64`/`iosSimulatorArm64` — без него Gradle не создаёт `.framework`, который embed-ит Xcode.

Больше в KMP-модулях трогать не нужно — дальше всё делается в Xcode.

## 1. Предварительные требования

- **macOS** + **Xcode 15 или новее** (Kotlin/Native-компилятор требует установленные Command Line Tools — `xcode-select --install`, если Xcode ставился не из App Store, либо запустите `sudo xcode-select --switch /Applications/Xcode.app`).
- **JDK 11+** (для Gradle; на Mac проще всего через Android Studio, у которой уже есть встроенный JBR).
- Проект уже пинует нужные версии в [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) — трогать не нужно:
  - Kotlin `2.2.21`
  - Compose Multiplatform `1.8.2`
  - kotlinx-datetime `0.6.1` (см. раздел «Известные грабли» ниже — это принципиально, не обновляйте её)
- Опционально: плагин **Kotlin Multiplatform Mobile** для Android Studio/Xcode — не обязателен, весь гайд ниже описывает создание проекта вручную, без него.

## 2. Создание Xcode-проекта

Проект создаётся вручную (без визарда KMM), т.к. репозиторий не начинался с
KMP-шаблона:

1. Откройте Xcode → **File → New → Project…**
2. Платформа **iOS**, шаблон **App**.
3. Заполните:
   - **Product Name**: `iosApp`
   - **Team**: ваш Apple ID / команда (для запуска на симуляторе можно оставить "None", для физического устройства — обязателен)
   - **Organization Identifier**: `uz.nodirbek`
   - **Bundle Identifier** получится `uz.nodirbek.iosApp` — **исправьте вручную** на `uz.nodirbek.flashcardsapp` (Signing & Capabilities → Bundle Identifier), чтобы совпадало с Android-версией пакета.
   - **Interface**: **SwiftUI**
   - **Language**: **Swift**
   - Тесты — не нужны, можно снять галочки Include Tests.
4. Сохраните проект **в корень репозитория, в новую папку `iosApp/`** (то есть рядом с `app/`, `composeApp/`, `shared/`, `gradle/`). Итоговый путь: `FlashCardsApp/iosApp/iosApp.xcodeproj`.

## 3. Embed KMP-фреймворка через Run Script Build Phase

Compose Multiplatform не поставляет CocoaPods/SPM-пакет "из коробки" для
такого проекта — фреймворк собирается Gradle-таской
`embedAndSignAppleFrameworkForXcode` и подключается вручную:

1. В Xcode откройте таргет **iosApp** → вкладка **Build Phases**.
2. Нажмите **+** → **New Run Script Phase**.
3. Перетащите новый шаг **выше** "Compile Sources" (порядок важен — фреймворк должен быть собран до компиляции Swift-кода, который его импортирует).
4. Вставьте скрипт:

   ```bash
   cd "$SRCROOT/.."
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```

   `$SRCROOT` — это `iosApp/` (папка .xcodeproj), поэтому `cd "$SRCROOT/.."` поднимает в корень репозитория, где лежит `gradlew`.

5. В **Build Phases → New Run Script Phase → Input Files** можно (не обязательно, но ускоряет инкрементальные сборки) добавить:
   ```
   $(SRCROOT)/../composeApp/build.gradle.kts
   ```

Таска `embedAndSignAppleFrameworkForXcode` сама читает переменные окружения,
которые выставляет Xcode при сборке, и не требует ручной настройки:

| Переменная (от Xcode) | Что определяет |
|---|---|
| `SDK_NAME` | симулятор (`iphonesimulator*`) или устройство (`iphoneos*`) → выбирает `iosSimulatorArm64`/`iosArm64` таргет |
| `CONFIGURATION` | `Debug`/`Release` → `KMP_FRAMEWORK_BUILD_TYPE` |
| `BUILT_PRODUCTS_DIR`, `TARGET_BUILD_DIR`, `FRAMEWORKS_FOLDER_PATH` | куда положить и подписать `.framework` |

Ничего из этого прописывать самому не нужно — Gradle-таска их уже читает.

## 4. Swift-код: ContentView.swift

Замените содержимое `ContentView.swift` на обёртку над Kotlin'овским
`MainViewController()`:

```swift
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
```

Пояснения:
- `import ComposeApp` — имя модуля берётся из `baseName = "ComposeApp"` в `composeApp/build.gradle.kts` (шаг 0).
- `MainViewControllerKt.MainViewController()` — Kotlin/Native экспортирует
  top-level `fun MainViewController()` из файла `MainViewController.kt` как
  статический метод класса `MainViewControllerKt` (стандартное поведение
  Kotlin/Native ObjC-интеропа для top-level функций — по имени файла + `Kt`).
- `.ignoresSafeArea(.all)` — Compose сам рисует под системными барами (как в
  Android-версии с edge-to-edge), поэтому safe area отдаём целиком под Compose.

В `iosApp/iosAppApp.swift` (точка входа SwiftUI-приложения, сгенерированная
шаблоном) ничего менять не нужно — она уже вызывает `ContentView()`.

## 5. Info.plist

Откройте `Info.plist` (или вкладку **Info** таргета) и убедитесь:
- **Minimum Deployments / iOS Deployment Target**: `14.0` — минимум, официально
  поддерживаемый Compose Multiplatform 1.8.x для iOS. Ставится и в
  **Build Settings → Deployment → iOS Deployment Target** таргета, значение
  должно совпадать.

## 6. Запуск

1. В Xcode выберите таргет `iosApp` и любой **симулятор** (например iPhone 15) в выпадающем списке устройств сверху.
2. **⌘R** (Run).
3. Первая сборка займёт заметно больше времени — Gradle качает Kotlin/Native-компилятор и компилирует framework "с нуля" (`downloadKotlinNativeDistribution` + полная компиляция `:shared`+`:composeApp` под `iosSimulatorArm64`).
4. Ожидаемый результат: запускается онбординг-карусель (свайп между страницами, как на Android), по завершении — экран с текстом "Полный экран приложения (навигация, данные) появится в Фазе 6."

Для **физического устройства**: выберите iPhone в списке устройств вместо
симулятора, таргет `iosArm64` соберётся автоматически через ту же Run
Script-фазу (по `SDK_NAME=iphoneos*`). Нужен настоящий **Team** в Signing &
Capabilities (шаг 2) — без него Xcode не подпишет билд для устройства.

## 7. Troubleshooting

**`framework not found ComposeApp` / линковка падает**
Run Script Phase не отработал или отработал с ошибкой. Откройте вкладку
**Report Navigator** (⌘9) → последний Run → разверните шаг с `gradlew` и
прочитайте реальный Gradle-вывод (частая причина — сеть недоступна при первой
загрузке Kotlin/Native-дистрибутива). Также проверьте, что Run Script Phase
стоит **выше** "Compile Sources" (шаг 3).

**`No such module 'ComposeApp'`**
После первого успешного прогона Run Script Phase Xcode иногда не
переиндексирует свежесобранный framework в рамках того же билда. Сделайте
**Product → Clean Build Folder** (⇧⌘K), затем **⌘R** заново.

**Ошибка code signing ("Signing for "iosApp" requires a development team")**
Только для запуска на физическом устройстве (см. шаг 6) — на симуляторе
подпись не нужна. Откройте **Signing & Capabilities** таргета и выберите
свой Apple ID в **Team**.

**`xcrun: error: unable to find utility "xcodebuild"` при сборке из Gradle**
Command Line Tools не установлены/не выбраны. Выполните:
```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
```

**`Unresolved reference 'System'` при сборке `:composeApp` под iOS**
Если вы видите эту ошибку при локальной пересборке — значит кто-то (скорее
всего случайно через `./gradlew --refresh-dependencies` или ручное
редактирование `libs.versions.toml`) снял форс версии kotlinx-datetime. См.
раздел ниже.

## 8. Известные грабли: kotlinx-datetime и Kotlin 2.2.21

При первой в этой миграции попытке скомпилировать `:composeApp` под iOS
(`./gradlew :composeApp:compileKotlinIosSimulatorArm64`) обнаружилась скрытая
несовместимость версий, из-за которой сборка падала с
`Unresolved reference 'System'` в доброй половине экранов (`StatsScreen`,
`HomeViewModel`, `AddWordBottomSheet` и др. — везде, где `Clock.System.now()`/
`.todayIn(...)`), хотя `:shared` под iOS всегда компилировался нормально.

**Причина:** `gradle/libs.versions.toml` пинует `kotlinx-datetime = "0.6.1"`,
но Compose Multiplatform Material3 (внутренний `DatePicker`) и/или `haze`
транзитивно требуют `kotlinx-datetime:0.7.1` — и Gradle по правилам
разрешения конфликтов молча поднимает версию для `:composeApp` (но не для
`:shared`, у которого нет этих зависимостей). В kotlinx-datetime 0.7.0 `Clock`/
`Instant` вообще выпилили в пользу `kotlin.time.Clock`/`Instant`; в 0.7.1 их
вернули как typealias на `kotlin.time.*` — но `kotlin.time.Clock.System`
помечен `@SinceKotlin("2.3")`, а проект закреплён на Kotlin **2.2.21**.
Отсюда и "unresolved" — на 2.2.21 такого API в стандартной библиотеке просто
нет.

**Фикс** (уже в `composeApp/build.gradle.kts`, ничего делать не нужно, если
не трогать эту секцию):

```kotlin
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-datetime:${libs.versions.kotlinxDatetime.get()}")
    }
}
```

Принудительно фиксирует `kotlinx-datetime` на `0.6.1` (той версии, на
которой написан весь текущий код) для **всех** конфигураций `:composeApp`,
включая транзитивные запросы от Material3/haze. Проверено — линковка
iOS-фреймворка при этом не ломается (Material3/haze используют
kotlinx-datetime только во внутренних `DatePicker`-компонентах, которые этот
проект не вызывает).

Если в будущем понадобится реально перейти на kotlinx-datetime 0.7.x —
это требует сначала поднять Kotlin до 2.3+, иначе `Clock.System` снова не
резолвится.

## 9. Что дальше (Фаза 6)

`AppContainer` и `NavGraph` уже общие (см. раздел 0) — навигация и основной
цикл изучения карточек на iOS работают. Осталось (не входит в объём этого
документа):
- Портировать 4 из 5 экранов, у которых пока только заглушка на iOS (`PlatformScreens.ios.kt`): **Настройки** (`SettingsScreen`), **Импорт колод** (`ImportScreen` — файловые пикеры), **поиск по AnkiWeb** (`AnkiWebBrowseScreen` — WebView-скрейпинг), **Open Trivia DB** (`OpenTDBBrowseScreen` — сетевой запрос). `PlatformHomeScreen` для iOS уже рабочий (упрощённый список колод), но со временем стоит сблизить с полной Android-версией `HomeScreen` (баннеры, streak, поиск).
- TTS (`TtsManager.ios.kt` сейчас no-op-заглушка) → реализация через `AVSpeechSynthesizer`.
- `HtmlText.ios.kt` (сейчас просто вырезает HTML-теги regex'ом) → полноценный рендеринг через `NSAttributedString`.
- `CardImportLauncher.ios.kt` (сейчас no-op) → `UIDocumentPickerViewController`.
- Уведомления (`notification/`-пакет, сейчас `AlarmManager`/`WorkManager`-only) → `UNUserNotificationCenter`.
- Сетевой слой `AnkiWebBrowser` (сейчас `WebView`-скрейпинг) → переход на Ktor Client, т.к. `android.webkit.WebView` не существует на iOS.
- Реальные шрифтовые файлы (.ttf) для Outfit/Inter — `Type.ios.kt` сейчас использует системный `FontFamily.Default`.
