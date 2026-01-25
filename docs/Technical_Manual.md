# Technical Manual (Vivi Music)

> [!IMPORTANT]
> This document is the SINGLE SOURCE OF TRUTH for the architecture, setup, and workflows of **Vivi Music Enterprise Edition**.

## 1. Architecture Overview (Mermaid)

```mermaid
graph TD
    User[User] --> UI[UI Layer (Compose)]
    UI --> ViewModel[Infrastructure Layer (ViewModels)]
    ViewModel --> Domain[Domain Layer (UseCases)]
    Domain --> Data[Data Layer (Repositories)]
    Data --> Remote[Remote Data (Innertube/Retrofit)]
    Data --> Local[Local Data (Room/DataStore)]
```

## 2. Tech Stack & Environment
*   **Language:** Kotlin 2.1
*   **JDK:** Java 21
*   **Build System:** Gradle 8.x (KTS)
*   **Minimum SDK:** 26 (Android 8.0)
*   **Target SDK:** 36 (Android 16)
*   **IDE:** Android Studio Ladybug or newer

## 3. Setup & Onboarding (Zero-to-Hero)
1.  **Clone the Repo:** `git clone ...`
2.  **Open in Android Studio.**
3.  **Sync Gradle.**
4.  **Run Tests:** `./gradlew testDebugUnitTest`

## 4. Workflows & Governance
### TDD (Red-Green-Refactor)
All new features MUST follow TDD.
1.  Write a failing test (Unit or E2E).
2.  Implement the minimal code to pass.
3.  Refactor.

### Code Style
We strictly enforce the **Google Android Style Guide** via `.editorconfig`.
*   Run `Code > Reformat Code` (Ctrl+Alt+L) before every commit.
*   Ensure no trailing whitespaces.

## 5. Module Deep Dives
### :app
The main application module containing UI and DI orchestration.

### :innertube
The core library interacting with the YouTube Music API.

## 6. Commands Cheat Sheet
*   **Run Unit Tests:** `./gradlew test`
*   **Run Kover Report:** `./gradlew koverHtmlReport`
*   **Generate Docs:** `./gradlew dokkaHtml`
