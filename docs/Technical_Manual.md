# Technical Manual (Vivi Music)

> [!IMPORTANT]
> This document is the **SINGLE SOURCE OF TRUTH** for the architecture, setup, and workflows of **Vivi Music Enterprise Edition**.

## 1. Architecture Overview
We follow a clean, modular architecture based on **Google's Guide to App Architecture**.

```mermaid
graph TD
    User[User Interaction] --> UI[UI Layer (Compose)]
    UI --> ViewModel[Infrastructure Layer (ViewModels)]
    ViewModel --> Domain[Domain Layer (UseCases)]
    Domain --> Data[Data Layer (Repositories)]
    Data --> Remote[Remote Data (Innertube/Retrofit)]
    Data --> Local[Local Data (Room/DataStore)]
```

### Key Components
- **UI:** Jetpack Compose (Single Activity, Single File Components).
- **State Management:** Unidirectional Data Flow (UDF) with `StateFlow`.
- **DI:** Hilt (Dependency Injection).
- **Testing:** TDD (Red-Green-Refactor) with JUnit 5, MockK, and Tribune.

## 2. Tech Stack & Environment
| Component | Specification |
| :--- | :--- |
| **Language** | Kotlin 2.3 |
| **JDK** | Java 21 |
| **Build System** | Gradle 9.x (KTS) |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 36 (Android 16) |
| **IDE** | Android Studio Ladybug or newer |
| **Code Style** | Google Android Style Guide (enforced via `.editorconfig`) |

## 3. Setup & Onboarding (Zero-to-Hero)
**Prerequisites:**
- Android Studio Ladybug+
- JDK 21 installed and configured in IDE
- Git

**Quick Start:**
1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/vivizzz007/vivi-music.git
    cd vivi-music
    ```
2.  **Open in Android Studio.**
3.  **Sync Gradle:** Ensure all dependencies download correctly.
4.  **Verify Setup:**
    ```bash
    ./gradlew testDebugUnitTest
    ```

## 4. Workflows & Governance
### TDD (Red-Green-Refactor)
All new features **MUST** follow TDD.
1.  **Red:** Write a failing test (Unit or E2E).
2.  **Green:** Implement the minimal code to pass.
3.  **Refactor:** Optimize and clean up.

### Verification Rules
- **Coverage:** Minimum **80%** code coverage for core business logic.
- **Linting:** Code must be "Linter-Ready" (no warnings) before commit.
- **Imports:** Strict canonical order (`android.*`, `androidx.*`, `com.*`, `java.*`, `kotlin.*`).

### Commits
- Use semantic commit messages (e.g., `feat: add news detail screen`, `fix: resolve crash on boot`).

## 5. Documentation
- **KDoc:** Required for all public classes and functions.
- **Architecture Decisions:** Documented in ADRs (Architecture Decision Records).
- **API Reference:** Generated via Dokka at `/docs/api`.

## 6. Commands Cheat Sheet
*   **Run Unit Tests:** `./gradlew test`
*   **Run Kover Report:** `./gradlew koverHtmlReport`
*   **Generate Docs:** `./gradlew dokkaHtml`
*   **Check Linting:** `./gradlew lintDebug`
