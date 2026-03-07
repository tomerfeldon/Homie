# Homie

An Android application for simplifying shared apartment management. Homie helps roommates coordinate expenses, tasks, inventory, and communication in one place.

## Features

### Authentication
- Email/password authentication via Firebase
- User registration with profile creation
- Seamless login flow with apartment setup

### Apartment Management
- Create new apartments with auto-generated invite codes
- Join existing apartments using 6-digit invite codes
- View apartment members and their profiles

### Dashboard
- Central hub displaying apartment overview
- Member list with avatars
- Quick view of urgent/uncompleted tasks
- Real-time financial balance summary

### Wallet (Expense Tracking)
- Record shared expenses with amount, category, and description
- Upload receipt images
- Categories: Rent, Groceries, Electricity, Internet, Maintenance, Other
- Automatic expense splitting among roommates
- Real-time balance calculation showing who owes whom

### Task Management
- Create and assign household chores
- Track task completion status
- Streak system for completed tasks
- Urgent tasks highlighted on dashboard

### Inventory
- Shared shopping/grocery list
- Track items needed with quantities
- Mark items as purchased
- Quick add expenses for purchased items

## Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

### Dependencies
- Firebase Authentication
- Firebase Firestore
- Firebase Cloud Storage
- AndroidX Material Design 3
- Jetpack Navigation Component
- Kotlin Coroutines
- Glide (Image Loading)
- LiveData & ViewModel

## Project Structure

```
app/src/main/java/com/example/homie/
├── MainActivity.kt
├── data/
│   ├── model/
│   │   ├── User.kt
│   │   ├── Apartment.kt
│   │   ├── Task.kt
│   │   ├── Expense.kt
│   │   ├── InventoryItem.kt
│   │   └── DashboardData.kt
│   └── repository/
│       ├── AuthRepository.kt
│       ├── ApartmentRepository.kt
│       ├── DashboardRepository.kt
│       ├── TasksRepository.kt
│       └── InventoryRepository.kt
└── ui/
    ├── auth/           # Login & Registration
    ├── onboarding/     # Apartment Setup
    └── main/
        ├── dashboard/  # Dashboard
        ├── wallet/     # Expenses
        ├── tasks/      # Task Management
        └── inventory/  # Inventory Tracking
```

## Setup

### Prerequisites
- Android Studio (latest version recommended)
- JDK 11 or higher
- Firebase account

### Firebase Configuration

1. Create a new project in [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name `com.example.homie`
3. Download `google-services.json` and place it in the `app/` directory
4. Enable the following Firebase services:
   - Authentication (Email/Password provider)
   - Firestore Database
   - Cloud Storage

### Build & Run

1. Clone the repository
   ```bash
   git clone <repository-url>
   cd Homie
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Run the app on an emulator or physical device

## Database Schema

### Users Collection
```
users/{userId}
├── userId: String
├── name: String
├── email: String
├── avatarUrl: String (optional)
├── apartmentId: String
└── streak: Int
```

### Apartments Collection
```
apartments/{apartmentId}
├── name: String
├── inviteCode: String
├── members: List<String>
├── tasks/{taskId}
│   ├── title: String
│   ├── description: String
│   ├── assignedTo: String
│   ├── completed: Boolean
│   └── timestamp: Long
├── expenses/{expenseId}
│   ├── amount: Double
│   ├── category: String
│   ├── description: String
│   ├── payerId: String
│   ├── receiptUrl: String (optional)
│   └── timestamp: Long
└── inventory/{itemId}
    ├── name: String
    ├── quantity: Int
    ├── addedBy: String
    ├── purchased: Boolean
    └── timestamp: Long
```

## License

This project is for educational purposes.