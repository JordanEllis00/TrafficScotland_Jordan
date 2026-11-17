# Traffic Scotland Monitor (MPD CW1)

Android app (Java, Android Studio) showing Traffic Scotland DATEX II **unplanned events** and **current roadworks**.

## Open and run
1. Android Studio → *Open* this folder (Gradle sync, JDK 17 as bundled with Android Studio).
2. Run on an emulator or phone (minSdk 26).
3. Live data needs Traffic Scotland DATEX II credentials. Copy `credentials.properties.example` to `credentials.properties`
   (git-ignored) and fill in the ID/key, **or** enter them in the app: overflow menu → *DATEX II API Credentials*.
   Without credentials the app still runs on cached / bundled sample data.

## Architecture (single activity + fragment, MVVM)
| Layer | Classes |
|---|---|
| Model | `TrafficSituation`, `SituationFilter` (pure Java, unit tested) |
| Parser | `DatexXmlParser` (XmlPullParser) |
| Repository | `TrafficRepository` (background executors, auto-refresh, live → cache → sample fallback per feed) |
| ViewModel | `TrafficViewModel` (filters, reference location, list/map mode as LiveData) |
| View | `MainActivity`, `SituationAdapter`, `SituationDetailBottomSheet` (fragment), `SituationStyle` |

Separate portrait and landscape `activity_main.xml` layouts.

## Tests
* Android Studio: right-click `app/src/test/java` → *Run tests* (JUnit 4, 38 tests).
* Command line (JDK only, no Android SDK): `bash testing/run_tests.sh`; evidence scripts are in `testing/`.

## Icons and attribution
* `ic_accident`, `ic_closure`, `ic_congestion`, `ic_obstruction`, `ic_roadworks`: Google Material Icons, Apache License 2.0.
* `ic_weather`, `ic_event`, `ic_info`: drawn for this project.
* Map tiles/data © OpenStreetMap contributors via osmdroid.
* Data: Traffic Scotland DATEX II v2.3 (Transport Scotland).
