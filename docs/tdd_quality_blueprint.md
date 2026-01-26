# TDD & Quality Blueprint

## 1. Testing Philosophy
"Test behavior, not implementation."

## 2. Test Pyramid
- **Unit Tests (70%)**: Business logic, ViewModels, Repositories (local).
  - Tools: JUnit 5, MockK, Turbine (Flows).
  - Coverage Goal: 80% instruction coverage for core logic.
- **Integration Tests (20%)**: Database (Room), Service interactions.
- **E2E Tests (10%)**: Critical User Journeys (CUJs).
  - Tools: Maestro (Recommended) or Compose Test Rule.

## 3. TDD Workflow
1.  **Red**: Write a failing test that defines the expected behavior (e.g., `initial state is LOADING`).
2.  **Green**: Write the minimal code to pass the test.
3.  **Refactor**: Clean up the code while keeping tests green.

## 4. Key Configurations
### Turbine (Flow Testing)
```kotlin
viewModel.state.test {
    assertEquals(Loading, awaitItem())
    assertEquals(Success(data), awaitItem())
}
```

### MockK (Isolation)
```kotlin
coEvery { repository.getData() } returns Result.success(data)
```

## 5. Continuous Integration (CI) Checks
Every PR must pass:
1.  `./gradlew ktlintCheck` (Style)
2.  `./gradlew testDebugUnitTest` (Logic)
3.  `./gradlew verifyKover` (Coverage - Future enforcement)
