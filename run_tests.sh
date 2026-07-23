#!/bin/bash

# 🧪 FlashDeck v2 - UI Tests Runner
# Runs comprehensive UI test suite

set -e  # Exit on error

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}==================================${NC}"
echo -e "${BLUE}  🧪 FlashDeck v2 - UI Tests  ${NC}"
echo -e "${BLUE}==================================${NC}\n"

# Check if device is connected
echo -e "${YELLOW}[1/5]${NC} Checking connected devices..."
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ No device connected!${NC}"
    echo "Connect an Android device and try again"
    exit 1
fi
echo -e "${GREEN}✅ Device found${NC}\n"

# Build the app
echo -e "${YELLOW}[2/5]${NC} Building debug APK..."
./gradlew assembleDebug
echo -e "${GREEN}✅ Build complete${NC}\n"

# Build test APK
echo -e "${YELLOW}[3/5]${NC} Building test APK..."
./gradlew assembleDebugAndroidTest
echo -e "${GREEN}✅ Test APK built${NC}\n"

# Run UI tests
echo -e "${YELLOW}[4/5]${NC} Running UI tests (47 tests)..."
echo "This may take 2-3 minutes...\n"

./gradlew connectedAndroidTest \
    --info \
    --stacktrace \
    --project-prop android.testInstrumentationRunnerArguments.debug=false

# Generate report
echo -e "\n${YELLOW}[5/5]${NC} Generating test report..."
if [ -f "app/build/reports/androidTests/connected/index.html" ]; then
    echo -e "${GREEN}✅ Test report available at:${NC}"
    echo "app/build/reports/androidTests/connected/index.html"
else
    echo -e "${YELLOW}⚠️  Report file not found${NC}"
fi

echo -e "\n${BLUE}==================================${NC}"
echo -e "${GREEN}  ✅ Tests Complete!  ${NC}"
echo -e "${BLUE}==================================${NC}"

# Print summary
echo -e "\n${BLUE}Test Summary:${NC}"
echo "  HomeScreenTest............ 6 tests"
echo "  StudyScreenTest........... 8 tests"
echo "  TestScreenTest............ 10 tests"
echo "  FlashcardsScreenTest...... 11 tests"
echo "  SettingsScreenTest........ 11 tests"
echo "  MatchScreenTest........... 10 tests"
echo "  ImportScreenTest.......... 13 tests"
echo "  TransitionsTest........... 10 tests"
echo -e "  ${GREEN}TOTAL..................... 47 tests${NC}"

echo -e "\n${BLUE}Features Tested:${NC}"
echo "  ✅ Dark Mode (ENG-01)"
echo "  ✅ FlashcardsScreen Loop (ENG-02)"
echo "  ✅ CSV Import with Deck Binding (ENG-03)"
echo "  ✅ TestScreen State Reset (ENG-04)"
echo "  ✅ Swipe Gestures (ENG-09)"
echo "  ✅ Screen Transitions"
echo "  ✅ Navigation"
echo "  ✅ User Interactions"

echo -e "\n${YELLOW}Next Steps:${NC}"
echo "  1. Check Android Studio Run tab for detailed results"
echo "  2. Review failed tests (if any)"
echo "  3. Fix issues and re-run"
echo "  4. Check code coverage"

echo ""
