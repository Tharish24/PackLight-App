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

## OpenRouter: public app without key prompts
The assistant uses one app-owned Cloudflare Worker. Users never enter provider keys.
Deploy backend/worker.mjs once, store OPENROUTER_API_KEY as a Cloudflare secret, and configure PACKLIGHT_AI_URL with the deployed HTTPS /chat URL.
See backend/README.md for exact deployment steps. No hosted endpoint is provisioned automatically by building the APK.
The previous per-device API key flow is retired; old device credentials are removed on launch.
Questions and selected country go through the Worker to OpenRouter and the model provider; trip notes and checklists are not sent.
The NVIDIA free endpoint logs requests. Do not send confidential or personal information.
Each chat goes directly to the model in one provider request, plus at most one retry for transient provider failures. PackLight's keyword blocklists and input/output moderation passes have been removed. Rate limits, account failures and safety refusals are not automatically retried. The gateway has a 75-second total budget; Android waits up to 90 seconds and offers Retry last question after a connection failure.
Shared free-model quotas apply to all app users. OpenRouter and the model provider may still enforce their own content restrictions.

## Deployed gateway
The shared Cloudflare Worker is deployed at https://packlight-ai.nbtharish.workers.dev.
The Android build uses its /chat endpoint through PACKLIGHT_AI_URL. OPENROUTER_API_KEY is stored only in Cloudflare Secrets.
Users do not enter keys, and the owner's computer does not need to be running.

## Services and credentials
Weather is wired to https://api.open-meteo.com/v1/forecast using the documented current variables and timezone=auto.
Attribution: Open-Meteo (https://open-meteo.com/), CC BY 4.0 weather data. The free endpoint is for non-commercial usage; review current Open-Meteo terms and obtain a commercial service agreement before commercial distribution.

Without a configured backend URL the app shows the offline travel helper. This fallback never asks users for keys.
To enable generative AI, deploy the included backend example behind HTTPS and set this nonsecret Gradle property before building:
PACKLIGHT_AI_URL=https://your-host.example/chat
Pass it with -PPACKLIGHT_AI_URL=https://your-host.example/chat or set it in your local Gradle properties.
Never put a model provider secret in this Android project or the APK.

Backend contract:
POST application/json with {"message":"question","destination":"Kyoto"}.
Return {"safe":true,"reply":"model response"} on success. The safe field is retained for compatibility with older APKs; it no longer represents an app-level moderation decision. HTTP errors use fixed diagnostic codes.

The Cloudflare Worker requires OPENROUTER_API_KEY and a deployed HTTPS URL. server.mjs is retained only as a legacy Gemini example.
Provider credentials remain on the server. See backend/README.md.
The Android client forwards questions and displays model replies without local keyword checks or content classification. Request size limits, rate limits and retry budgets remain.
Messages are held in memory and not written to disk; when AI is enabled, the current question and selected destination are sent to the configured backend. Trips and packing notes are never sent.

## Verification coverage
Validated in Android Studio: assembleDebug, lintDebug, testDebugUnitTest (12 passed), and connectedDebugAndroidTest on emulator-5554 (2 passed, including a trip/itinerary/checklist storage round trip). Phone and tablet previews rendered; app deployed successfully.
Lint has no errors; remaining template resource qualifier warnings are non-blocking.
The Cloudflare Worker is deployed and live requests were verified. Backend tests mock the provider to cover direct question/response forwarding, bounded retries, deadline exhaustion, quota errors and HTTP-200 embedded errors.
Unit tests cover date validation, country search, connection error messages, difficulty bounds and collision crossing.
Phone/tablet Compose previews are included in MainActivity.kt.
Suggested device checks: create a trip and checklist, force stop/relaunch and verify data, rotate while editing, test large font and TalkBack, refresh weather online/offline, and play/pause/reopen the game.
Guide climate ranges are approximate editorial summaries. Verify opening hours, travel advisories and local rules before departure.

## Source layout
TravelData.kt — destination content and DataStore repository
TravelViewModel.kt — state, assistant routing and HTTP services
MainActivity.kt — theme, adaptive shell and travel screens
GameScreen.kt — game loop, collision and lifecycle handling
backend/ — optional real AI service example
