# PackLight

Native Kotlin / Jetpack Compose / Material 3 travel companion for Android 8.0 (API 26) and later.

## Open and build
Extract the entire ZIP, open its root folder in Android Studio, let Gradle sync, and run the app module.
Use Android Studio's embedded JDK (17 or later; the bundled JDK is recommended), Android SDK 37, and an internet connection for the first dependency download.
The Gradle wrapper is included.

Validation:
- gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
- APK: app/build/outputs/apk/debug/app-debug.apk
- Lint: app/build/reports/lint-results-debug.html

## Features
- Create, edit and delete trips with validated date ranges, itinerary entries and reservation notes.
- Separate packing checklist per trip; add, remove and check items.
- DataStore saves trips, checklists and high scores atomically on device. Data is not sent to an account or cloud database. Android system backup follows the included backup XML.
- Offline country autocomplete across ISO countries and territories plus Kosovo, with prefix/substring search, country codes and common aliases (UK, UAE, USA). Available in the assistant, Explore and trip editor. Countries named in questions override the current selection.
- Four detailed offline city guides: Kyoto, Lisbon, Dubai and Reykjavík. Other countries are selectable and receive general offline planning help; missing detailed guides are identified explicitly.
- Real Open-Meteo current conditions, manual refresh, observation and retrieval timestamps, and explicit network failure messages. No location permission; requests use the selected guide's coordinates.
- Assistant with safe curated offline answers and an optional HTTPS generative AI backend.
- Carry-on Dash: offline endless lane game, bounded increasing speed, pause/resume, keyboard arrows, saved high score.
- Phone bottom navigation, tablet/desktop rail, bounded content width, scrollable layouts, system dark mode, Material accessibility semantics and system insets. User input and selected sections survive recreation. The game pauses when backgrounded or leaving its tab.
- No analytics, advertisements, sign-in or embedded API secrets.

## Services and credentials
Weather is wired to https://api.open-meteo.com/v1/forecast using the documented current variables and timezone=auto.
Attribution: Open-Meteo (https://open-meteo.com/), CC BY 4.0 weather data. The free endpoint is for non-commercial usage; review current Open-Meteo terms and obtain a commercial service agreement before commercial distribution.

No AI endpoint or provider credentials were present in the original project. The application therefore identifies itself as OFFLINE GUIDE MODE. This is a curated intent matcher, not a generative model.
To enable generative AI, deploy the included backend example behind HTTPS and set this nonsecret Gradle property before building:
PACKLIGHT_AI_URL=https://your-host.example/chat
Pass it with -PPACKLIGHT_AI_URL=https://your-host.example/chat or set it in your local Gradle properties.
Never put a model provider secret in this Android project or the APK.

Backend contract:
POST application/json with {"message":"question","destination":"Kyoto"}.
Return {"safe":true,"reply":"moderated travel response"} only after input and output moderation succeeds.
Return {"safe":false} for a denied request. Invalid, missing-safe and failing responses do not display generated content.

The included backend requires GEMINI_API_KEY, a supported GEMINI_MODEL, and a deployed HTTPS URL.
Provider credentials remain on the server. See backend/README.md.
The client checks both input and output for common harmful/offensive terms (including some obfuscation).
Local word checks are defense in depth and cannot guarantee semantic safety; production server moderation is mandatory.
Offline mode returns only curated text and does not echo denied prompts.
Messages are held in memory and not written to disk; when AI is enabled, the current question and selected destination are sent to the configured backend. Trips and packing notes are never sent.

## Verification coverage
Validated in Android Studio: assembleDebug, lintDebug, testDebugUnitTest (5 passed), and connectedDebugAndroidTest on emulator-5554 (2 passed, including a trip/itinerary/checklist storage round trip). Phone and tablet previews rendered; app deployed successfully.
Lint has no errors; remaining template resource qualifier warnings are non-blocking.
The optional AI gateway has not been executed because Node.js and provider credentials are absent.
Unit tests cover date validation, language filtering, difficulty bounds and collision crossing.
Phone/tablet Compose previews are included in MainActivity.kt.
Suggested device checks: create a trip and checklist, force stop/relaunch and verify data, rotate while editing, test large font and TalkBack, refresh weather online/offline, and play/pause/reopen the game.
Guide climate ranges are approximate editorial summaries. Verify opening hours, travel advisories and local rules before departure.

## Source layout
TravelData.kt — destination content and DataStore repository
TravelViewModel.kt — state, safe assistant routing and HTTP services
MainActivity.kt — theme, adaptive shell and travel screens
GameScreen.kt — game loop, collision and lifecycle handling
backend/ — optional real AI service example
