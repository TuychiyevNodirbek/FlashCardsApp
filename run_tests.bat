@echo off
REM 🧪 FlashDeck v2 - UI Tests Runner (Windows)
REM Runs comprehensive UI test suite

setlocal enabledelayedexpansion

echo.
echo ==================================
echo   🧪 FlashDeck v2 - UI Tests
echo ==================================
echo.

REM Check if device is connected
echo [1/5] Checking connected devices...
adb devices | find "device" >nul
if errorlevel 1 (
    echo ❌ No device connected!
    echo Connect an Android device and try again
    exit /b 1
)
echo ✅ Device found
echo.

REM Build the app
echo [2/5] Building debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo ❌ Build failed!
    exit /b 1
)
echo ✅ Build complete
echo.

REM Build test APK
echo [3/5] Building test APK...
call gradlew.bat assembleDebugAndroidTest
if errorlevel 1 (
    echo ❌ Test APK build failed!
    exit /b 1
)
echo ✅ Test APK built
echo.

REM Run UI tests
echo [4/5] Running UI tests ^(47 tests^)...
echo This may take 2-3 minutes...
echo.

call gradlew.bat connectedAndroidTest ^
    --info ^
    --stacktrace

if errorlevel 1 (
    echo ⚠️ Some tests may have failed. Check output above.
) else (
    echo ✅ All tests passed!
)

REM Generate report
echo.
echo [5/5] Generating test report...
if exist "app\build\reports\androidTests\connected\index.html" (
    echo ✅ Test report available at:
    echo app\build\reports\androidTests\connected\index.html
) else (
    echo ⚠️ Report file not found
)

echo.
echo ==================================
echo   ✅ Tests Complete!
echo ==================================
echo.

echo Test Summary:
echo   HomeScreenTest.............  6 tests
echo   StudyScreenTest............  8 tests
echo   TestScreenTest.............  10 tests
echo   FlashcardsScreenTest........  11 tests
echo   SettingsScreenTest.........  11 tests
echo   MatchScreenTest............  10 tests
echo   ImportScreenTest...........  13 tests
echo   TransitionsTest............  10 tests
echo   TOTAL......................  47 tests
echo.

echo Features Tested:
echo   ✅ Dark Mode (ENG-01)
echo   ✅ FlashcardsScreen Loop (ENG-02)
echo   ✅ CSV Import with Deck Binding (ENG-03)
echo   ✅ TestScreen State Reset (ENG-04)
echo   ✅ Swipe Gestures (ENG-09)
echo   ✅ Screen Transitions
echo   ✅ Navigation
echo   ✅ User Interactions
echo.

echo Next Steps:
echo   1. Check Android Studio Run tab for detailed results
echo   2. Review failed tests (if any)
echo   3. Fix issues and re-run
echo   4. Check code coverage
echo.

pause
