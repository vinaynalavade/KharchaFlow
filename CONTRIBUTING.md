# Contributing to Leaf

Thank you for your interest in contributing to Leaf! We welcome contributions from the community to make Leaf the best open-source, privacy-first personal finance application for Android.

Please take a moment to review this guide before submitting issues or pull requests.

---

## 🛠️ Development Environment & Setup

### Prerequisites
- **Java Development Kit:** OpenJDK 17 or OpenJDK 21
- **Android Studio:** Android Studio Ladybug (2024.2.1) or newer
- **Android SDK:** Android 15 (API level 35) SDK platform and build-tools installed
- **Git:** Version 2.30 or higher

### Getting Started
1. **Fork & Clone:**
   ```bash
   git clone https://github.com/vinaynalavade/Leaf.git
   cd Leaf
   ```

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Select **Open** and choose the `Leaf` root folder.
   - Allow Gradle to sync dependencies and generate KSP sources.

3. **Building the Project:**
   - Contributors **do not need** any production signing keys or release passwords.
   - The default `debug` build type uses standard Android debug keys generated automatically on your machine.
   ```bash
   # Assemble Debug APK
   ./gradlew assembleDebug
   ```

4. **Running Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 📐 Architecture & Coding Guidelines

Leaf follows a strict **Clean 4-Layer Architecture** to keep business logic independent of UI and platform frameworks:

```text
com.vinaynalavade.expensetracker
├── core/           # Pure domain models (Amount, Currency), Result taxonomy, Utils
├── data/           # Room Database, DAOs, Entities, DataStore, Repository implementations
├── domain/         # Pure Kotlin models, Repository interfaces, Use cases, Validators
├── presentation/   # Compose UI, ViewModels, Themes, Navigation, Widgets
└── di/             # Manual DI Container (AppContainer)
```

### Key Principles:
- **Precision Monetary Values:** Never use floating-point types (`Float` / `Double`) for money. Always use `Amount` (which wraps integer subunit `Long`).
- **Offline-First:** Leaf is 100% offline. Do not introduce proprietary servers, analytics, or third-party telemetry.
- **Compose UI Standards:** Keep Composables stateless where possible, hoist state to ViewModels, and utilize Material 3 theme tokens (`MaterialTheme.spacing`, `MaterialTheme.colorScheme`).
- **Database & DataStore:** Room migrations must be written explicitly for any schema alterations. DataStore preferences must maintain backward compatibility.
- **Kotlin Style:** Follow official Kotlin coding conventions (`camelCase` for functions/variables, `PascalCase` for classes/composables).

---

## 🔀 Submitting Pull Requests

1. **Create a Feature Branch:**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```
2. **Commit Guidelines:**
   - Write clear, meaningful commit messages (e.g. `feat: add export statement filter by category`, `fix: prevent duplicate category naming`).
3. **Verify Before Pushing:**
   - Ensure the project compiles cleanly without warnings or errors.
   - Run unit tests: `./gradlew testDebugUnitTest`.
4. **Open a Pull Request:**
   - Describe the problem solved or feature added.
   - Include screenshots or short screen recordings for any UI changes.
   - Reference any related open issue numbers.

---

## 🐛 Reporting Issues

- **Bug Reports:** Search existing issues first to avoid duplicates. When filing a new bug, include Android version, device model, steps to reproduce, expected vs actual behavior, and error logs if applicable.
- **Feature Requests:** Open an issue outlining the user story, proposed solution, and why it benefits offline personal finance management.

---

## 🔒 Security Reports

If you discover a security vulnerability or sensitive data leakage, please do **NOT** open a public issue. Refer to [SECURITY.md](SECURITY.md) for private reporting instructions.

---

## 📄 License

By contributing to Leaf, you agree that your contributions will be licensed under the **GNU General Public License v3.0 (GPLv3)**.
