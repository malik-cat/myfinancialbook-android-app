# My Financial Book - Android App

**My Financial Book** is a comprehensive, multi-language business management and accounting application built with **Jetpack Compose**. It empowers small to medium-sized businesses to manage their finances, inventory, staff, and invoices all in one place, with seamless cloud synchronization and robust security features.

---

## 🚀 Key Features

### 🏦 Financial Management
- **Cash Book**: Track daily cash-in and cash-out transactions.
- **Ledger (Khata)**: Manage customer and supplier accounts with ease.
- **Expense Tracker**: Categorize and monitor business expenses to optimize spending.

### 📦 Inventory & POS
- **Stock Management**: Keep track of your products, quantities, and pricing.
- **Point of Sale (POS)**: A simplified interface for quick sales and transaction recording.
- **Product Showcase**: View and manage your product catalog with image support.

### 📄 Billing & Invoices
- **Invoice Generation**: Create professional invoices and bills for your customers.
- **PDF & Excel Export**: Generate detailed financial reports in PDF and XLSX formats for offline use or sharing.

### 👥 Staff Management
- **Employee Records**: Manage staff details, attendance, or payroll-related information within the app.

### ☁️ Sync & Security
- **Cloud Sync**: Real-time data synchronization using **Firebase Firestore**.
- **Google Drive Backup**: Securely backup and restore your data using your personal Google Drive.
- **Biometric Security**: Protect your financial data with Fingerprint unlock and a mandatory App PIN.

### 🌍 Multi-language Support
The app is localized in 8+ languages to cater to a global audience:
- English, Urdu, Arabic, Hindi, Turkish, Spanish, Portuguese, and French.

---

## 🛠 Tech Stack

### Android (Mobile)
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with Material 3
- **Database**: [Room](https://developer.android.com/training/data-storage/room) for local persistence
- **Backend**: [Firebase Auth](https://firebase.google.com/docs/auth) & [Firestore](https://firebase.google.com/docs/firestore)
- **Storage/Sync**: [Google Drive API](https://developers.google.com/drive)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Utilities**: 
  - [ML Kit](https://developers.google.com/ml-kit) (QR Code Scanning)
  - [CameraX](https://developer.android.com/training/camerax) for barcode scanning
  - [Biometric Library](https://developer.android.com/training/sign-in/biometric-auth) for security

### Web Dashboard
- **Frontend**: HTML5, CSS3 (Custom Responsive Design), JavaScript
- **Integration**: Designed to work with the same Firebase backend for cross-platform data access.

---

## 📁 Project Structure

```text
myfinancialbook/
├── app/                        # Android application module
│   ├── src/main/java/          # Kotlin source code
│   │   └── com/myfinancialbook/app/
│   │       ├── data/           # Room Database, DAOs, and Repositories
│   │       ├── export/         # PDF and Excel export logic
│   │       ├── sync/           # Firebase and Google Drive sync managers
│   │       ├── ui/             # Jetpack Compose screens and components
│   │       └── util/           # Security, Auth, and Voice utilities
│   └── src/main/res/           # Resources (Drawables, Strings, Layouts)
├── web-dashboard/              # Companion web application
└── build.gradle.kts            # Project-level build configuration
```

---

## ⚙️ Getting Started

### Prerequisites
- [Android Studio Jellyfish](https://developer.android.com/studio) or newer.
- JDK 17.
- A Firebase Project (for cloud features).
- Google Cloud Console project with Drive API enabled (for backup features).

### Setup Instructions
1. **Clone the repository**:
   ```bash
   git clone https://github.com/malik-cat/myfinancialbook-android-app.git
   ```
2. **Firebase Configuration**:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `com.myfinancialbook.app`.
   - Download the `google-services.json` and place it in the `app/` directory.
   - Enable **Email/Google Authentication** and **Firestore Database**.
3. **Google Drive API**:
   - Enable the Google Drive API in your Google Cloud Console.
   - Configure the OAuth consent screen and create an Android OAuth 2.0 Client ID.
4. **Build and Run**:
   - Open the project in Android Studio.
   - Sync Gradle files and run the app on an emulator or physical device.

---

## 🔒 Supply-Chain Security (SLSA)

This project is hardened against software supply-chain attacks.

- **SLSA Build Level 3 provenance** — Every tagged release (`v*`) is built on
  GitHub-hosted runners and signed by the
  [SLSA GitHub Generator](https://github.com/slsa-framework/slsa-github-generator).
  The resulting `provenance.intoto.jsonl` attestation is attached to the
  GitHub release and can be verified with
  [`slsa-verifier`](https://github.com/slsa-framework/slsa-verifier).
- **Dependency hygiene** — [Dependabot](.github/dependabot.yml) keeps
  dependencies current, and
  [Dependency Review](.github/workflows/dependency-review.yml) blocks PRs
  that add known-vulnerable dependencies.
- **Continuous posture checks** — The [Scorecard](.github/workflows/scorecard.yml)
  workflow evaluates repository security and uploads results to the
  Security > Code scanning tab.
- **Reporting** — See [SECURITY.md](SECURITY.md) for the vulnerability
  reporting process and recommended branch protection rules.

> **CI build note:** the Gradle build requires `google-services.json`.
> In the SLSA workflow this is injected from the `GOOGLE_SERVICES_JSON`
> repository secret — see [SECURITY.md](SECURITY.md).

---

## 📸 Screenshots
*(Coming Soon)*
> You can find visual assets in the `app/src/main/res/drawable` folder, including the app icon and feature graphics.

---

## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License
This project is for educational/portfolio purposes. Please contact the author for licensing queries.
