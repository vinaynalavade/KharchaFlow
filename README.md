# KharchaFlow

> **Personal Expense Tracker for Android**

KharchaFlow is a modern, fast, 100% offline-first personal finance and expense tracking application built natively for Android using modern Jetpack Compose and Kotlin. It is designed to give you complete control over your personal finances with zero advertisements, zero third-party trackers, zero cloud servers, and strict on-device data privacy.

---

## 🌟 Key Principles & Features

### 🔒 100% Offline-First & Strictly Private
- **Zero Network Permissions:** All financial records, recurring schedules, and preferences are stored exclusively on your device in local SQLite/Room and Preferences DataStore.
- **Privacy by Design:** No accounts required, no telemetry, no analytics SDKs, no advertising libraries, and no third-party cloud synchronization.

### 💰 Precision Monetary Engine
- **Zero Rounding Errors:** Built on a specialized `@JvmInline value class Amount` modeling currency in exact integer subunits (e.g., paise/cents as `Long`), preventing floating-point rounding discrepancies in financial statements and ledger totals.

### 📊 Complete Financial Management
- **Transaction Lifecycle:** Create, view, edit with preloaded forms, and delete transactions with instant non-destructive undo snackbars.
- **Month-wise Grouping & Search:** Grouped automatically by month and day ("Today", "Yesterday", exact dates). Real-time search across notes and categories with instant type filters (All / Expense / Income).
- **Custom Categories with Duplicate Protection:** Create custom categories with custom icons and curated colors. Includes strict case-insensitive duplicate protection and protected system defaults to prevent orphaned transactions.
- **Financial Continuity & Carry-Forward:** Set a base opening balance and automatically calculate month-to-month carry-forward surplus/deficit into the next month's opening ledger balance.
- **Monthly Summary & Ledger:** Dedicated summary screen with month selector (`< Month >`), opening balance, total income, total expenses, net savings, closing balance, and categorized spending progress.
- **Day-Wise Calendar View:** Visualize daily income and expense distribution directly on an interactive calendar.
- **Recurring Transactions, Salary & EMI:** Schedule monthly salaries, subscriptions, and EMIs with deterministic duplicate-proof processing and optional due-date reminder alerts.
- **Smart Evening Reminder (9:00 PM):** Intelligent daily reminder that automatically checks if any transactions were recorded today; if already recorded, the reminder is quietly suppressed.
- **Professional On-Device PDF Statements:** Generate bank-quality transaction statements directly on device using Android's native `PdfDocument` engine with running balance calculations and share via system share sheet.
- **Multi-Currency Support:** Seamlessly switch between global currencies with exact subunit scaling:
  - Indian Rupee (`INR ₹`)
  - US Dollar (`USD $`)
  - Euro (`EUR €`)
  - British Pound (`GBP £`)
  - UAE Dirham (`AED د.إ`)
  - Singapore Dollar (`SGD S$`)
  - Canadian Dollar (`CAD C$`)
  - Australian Dollar (`AUD A$`)
  - Japanese Yen (`JPY ¥`)
- **Android Home-Screen Widgets:**
  - *Financial Overview Widget:* Glanceable total balance, today's spending, and 1-tap quick action buttons.
  - *Quick Add Widget:* Fast 1-tap transaction launcher.
- **Modern Material 3 Design:** Edge-to-edge UI with System Default, Light, and Dark theme modes with smooth micro-animations.

---

## 🛠️ Technology Stack & Architecture

- **Language:** Kotlin (100%)
- **UI Framework:** Jetpack Compose with Material Design 3
- **Local Persistence:** Room (SQLite) with TypeConverters & Kotlin Symbol Processing (KSP)
- **Preferences:** Jetpack DataStore Preferences
- **Asynchronous Flow:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Navigation:** Jetpack Navigation Compose
- **PDF Generation:** Native Android `android.graphics.pdf.PdfDocument` (Zero external PDF libraries)
- **Dependency Injection:** Container-based manual Dependency Injection (`AppContainer`)
- **Architecture:** Clean 4-Layer Architecture:
  ```text
  com.vinaynalavade.expensetracker
  ├── core/               # Domain-agnostic models (Amount, Currency), Result taxonomy, Utils
  ├── data/               # Room Database, DAOs, Entities, DataStore, Repository implementations
  ├── domain/             # Business models, Repository interfaces, Use cases, Validators
  ├── presentation/       # Compose UI screens, ViewModels, Themes, Navigation, Widgets
  └── di/                 # Application dependency container
  ```

---

## 📋 System Requirements & Permissions

### Requirements
- **Minimum Android Version:** Android 8.0 (API level 26 - Oreo)
- **Target Android Version:** Android 15 (API level 35)
- **JDK Requirement:** OpenJDK 17 or 21

### Permissions Used
- `android.permission.POST_NOTIFICATIONS` — Requested at runtime on Android 13+ only when daily reminders or recurring EMI notifications are enabled in Settings.
- `android.permission.RECEIVE_BOOT_COMPLETED` — Used to re-register scheduled alarm reminders upon device reboot.
- **`android.permission.INTERNET` is NOT requested or used.** The app has zero network access.

---

## 🚀 Building from Source

### 1. Prerequisites
- [Android Studio Ladybug (2024.2.1+)](https://developer.android.com/studio) or Android Command-line Tools
- OpenJDK 17 or 21
- Android SDK Build-Tools 35.0.0+

### 2. Clone the Repository
```bash
git clone https://github.com/vinaynalavade/kharchaflow.git
cd kharchaflow
```

### 3. Build Debug APK
To assemble the debug build:
```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows PowerShell / Command Prompt
.\gradlew.bat assembleDebug
```
The debug APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Run Unit Tests
```bash
# On Linux / macOS
./gradlew testDebugUnitTest

# On Windows PowerShell / Command Prompt
.\gradlew.bat testDebugUnitTest
```

---

## 🔑 Release Build & Signing Configuration

Release builds in KharchaFlow use R8 code shrinking, resource minification, and strict ProGuard rules.

Release signing credentials are kept strictly local and are never committed to the repository.

### Configuring Local Release Signing
You can configure your release signing in any of the following ways:

#### Option A: Local Properties File (Outside Git)
Create a file at `~/.android/kharchaflow-signing.properties` (or copy `signing.properties.example` to `signing.properties` in the project root):
```properties
STORE_FILE=/path/to/your/upload-keystore.jks
STORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

#### Option B: Environment Variables
Export the following environment variables:
```bash
export KHARCHAFLOW_KEYSTORE_PATH="/path/to/your/upload-keystore.jks"
export KHARCHAFLOW_KEYSTORE_PASSWORD="your_keystore_password"
export KHARCHAFLOW_KEY_ALIAS="your_key_alias"
export KHARCHAFLOW_KEY_PASSWORD="your_key_password"
```

### Build Production Android App Bundle (AAB)
```bash
# On Linux / macOS
./gradlew clean bundleRelease

# On Windows PowerShell / Command Prompt
.\gradlew.bat clean bundleRelease
```
The output will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

*(Note: If no release signing credentials are provided, Gradle will build an unsigned release bundle suitable for local verification.)*

---

## 🤝 Contributing

Contributions, bug reports, and feature proposals are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on code style, development workflow, and pull request guidelines.

---

## 🛡️ Security

For instructions on reporting security vulnerabilities, please refer to our [Security Policy](SECURITY.md).

---

## 📄 License

```text
KharchaFlow - Personal Expense Tracker for Android
Copyright (C) 2026 Vinay Nalavade

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
```

See the [LICENSE](LICENSE) file for the full license text.
