# 🏠 Homie

**Homie** is a native Android app that helps roommates manage their shared apartment — track expenses, split bills, assign chores, manage inventory, and stay in sync with real-time notifications.

---

## Features

- **Dashboard** — overview of your apartment: members, urgent tasks, and financial balance at a glance
- **Wallet** — log shared expenses by category, auto-split among roommates, view balances, and settle up
- **Tasks** — create and assign household chores, track completion, and build streaks
- **Inventory** — maintain a shared shopping list, mark items as purchased, and quickly log their cost
- **Notifications** — real-time push notifications for expenses, task updates, and settlements
- **Apartment Management** — create or join an apartment with a 6-digit invite code

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM |
| Database | Firebase Firestore |
| Authentication | Firebase Auth (Email/Password) |
| Storage | Firebase Cloud Storage |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| UI | Material Design 3, Jetpack Navigation |
| Image Loading | Glide |
| Async | Kotlin Coroutines + LiveData |
| Build | Gradle 8 with Kotlin DSL |

---

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- A Firebase project

### Firebase Setup

1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project
2. Add an Android app with package name: `com.example.homie`
3. Download `google-services.json` and place it inside the `/app` directory
4. Enable the following Firebase services:
   - **Authentication** → Email/Password provider
   - **Firestore Database** → production mode
   - **Cloud Storage** → for receipt/image uploads
   - **Cloud Messaging** → for push notifications

### Build & Run

```bash
git clone https://github.com/tomerfeldon/homie.git
cd Homie
./gradlew assembleDebug
```

Then open the project in Android Studio and run it on an emulator or physical device.

---

## Architecture

Homie follows the **MVVM** pattern with a clean separation of concerns:

```
UI (Fragments / Activities)
        ↓ observes
   ViewModel (LiveData / UiState)
        ↓ calls
   Repository (Firebase Firestore / Auth / Storage)
        ↓ reads/writes
   Firebase Backend
```

- **Model** — data classes in `data/model/`
- **Repository** — all Firebase operations in `data/repository/`
- **ViewModel** — UI state management with sealed `UiState` classes
- **View** — Fragments and Activities that observe ViewModels

---

## Project Structure

```
app/src/main/java/com/example/homie/
├── data/
│   ├── model/          # Data classes (User, Task, Expense, InventoryItem, …)
│   └── repository/     # Firebase data access layer
├── ui/
│   ├── auth/           # Login, Register, Splash
│   ├── onboarding/     # Create / Join apartment
│   └── main/
│       ├── dashboard/
│       ├── wallet/
│       ├── tasks/
│       ├── inventory/
│       └── notifications/
└── MainActivity.kt     # Navigation host
```

---

## License

This project was built for educational purposes.
