# Homie App — Top 5 Features Design Spec
**Date:** 2026-03-14
**Status:** Approved (rev 2)

---

## Overview

Five coordinated features added to the Homie Android roommate app. All use existing MVVM + Firebase Firestore architecture, XML layouts (no Jetpack Compose), and the Navigation Component.

---

## Feature 1 — Forgot Password (G1)

### Scope
`activity_login.xml`, `LoginActivity.kt`

### Design
- Add a "Forgot password?" `TextView` link below the login button in `activity_login.xml`
- Tapping it opens a `MaterialAlertDialog` with a single email `TextInputLayout`
- On confirm: call `FirebaseAuth.sendPasswordResetEmail(email)`
  - Success → toast: "Check your email for a reset link"
  - Failure → toast: error message from Firebase
- No new Fragment, ViewModel, or Repository needed — handled entirely in `LoginActivity`

---

## Feature 2 — Share Invite Code (G6)

### Scope
`fragment_dashboard.xml`, `DashboardFragment.kt`, `DashboardData.kt`, `DashboardUiState.kt`, `DashboardRepository.kt`

### Design
- Add an invite code row below `tvApartmentName` in `fragment_dashboard.xml`:
  - `TextView` showing "Invite code: XXXXXX"
  - Copy icon `ImageView` button to the right
- Tapping the copy icon copies the code to clipboard and shows toast: "Invite code copied!"
- **Data flow:** `DashboardData` gains `inviteCode: String` field. `DashboardRepository` already fetches the full `Apartment` object — extract `apartment.inviteCode` and pass it into the `DashboardData(...)` constructor call inside `getFullDashboardData()`. `DashboardUiState.Success` gains an `inviteCode: String` field. `DashboardFragment` binds it on success.

---

## Feature 3 — Assign Task to Roommate (T1)

### Scope
`fragment_add_task.xml`, `AddTaskFragment.kt`, `TasksViewModel.kt`, `TasksRepository.kt`, `Task.kt`

### Design
- Add a `createdBy: String` field to the `Task` data model to record the UID of whoever created the task (distinct from `assignedTo`). Set to `currentUser.uid` in `TasksRepository.addTask()`.
- Add an "Assign to" `Spinner` to the Add Task form, below the description field
- On fragment creation, load apartment members via a new `TasksViewModel.loadMembers()` / `TasksRepository.getMembers(aptId)` method
- Spinner defaults to the current user (index 0); user may select any member
- On save, `task.assignedTo` and `task.assignedToName` are set from the selected spinner item
- **`addTask()` signature change:** `TasksRepository.addTask()` and `TasksViewModel.addTask()` gain two new parameters: `assignedToId: String` and `assignedToName: String`, replacing the hardcoded `currentUser.uid` assignment

---

## Feature 4 — Swipe to Delete (W2 + T2 + I1)

### Scope
`WalletFragment.kt`, `TasksFragment.kt`, `InventoryFragment.kt`,
`WalletRepository.kt` (new), `TasksRepository.kt`, `InventoryRepository.kt`

### Design

#### Gesture
- `ItemTouchHelper` configured for `LEFT` swipe, attached to each RecyclerView in the host Fragment
- Red delete background with a trash icon drawn via `onChildDraw` during swipe

#### Permission Check
Before showing confirmation, compare current user UID against the item's **creator** field:

| Screen | Creator field |
|---|---|
| Wallet | `expense.payerId` |
| Tasks | `task.createdBy` (new field added in Feature 3) |
| Inventory | `inventoryItem.addedBy` |

**Implementation order note:** Feature 3 (which adds `task.createdBy`) must be implemented before or alongside Feature 4. The swipe-delete for Tasks uses `task.createdBy`, not `task.assignedTo`.

- If UID does not match: item snaps back, toast: "You can only delete items you created"
- If UID matches: show `MaterialAlertDialog` ("Delete this item? This cannot be undone")

#### On Confirm
- Item is deleted from Firestore
- On cancel: `notifyItemChanged(position)` to snap item back

#### Repository Methods
- `WalletRepository.deleteExpense(aptId, expenseId)` — Firestore document delete
- `TasksRepository.deleteTask(aptId, taskId)` — Firestore document delete
- `InventoryRepository.deleteInventoryItem(aptId, itemId)` — Firestore document delete

Tasks and Inventory use real-time listeners — list updates automatically after delete. Wallet calls `loadExpenses()` after delete to refresh.

---

## Feature 5 — Settle Up (W5)

### Scope
`fragment_wallet.xml`, `WalletFragment.kt`, `WalletViewModel.kt`, `WalletUiState.kt`,
new `WalletRepository.kt`, new `SettleUpBottomSheet.kt`,
new `BalanceSummaryAdapter.kt`, new `item_balance_row.xml`,
`Expense.kt`

### Design

#### New `MemberBalance` Data Class
A new data class placed in `com.example.homie.data.model` (alongside `User`, `Expense`, etc.):
```kotlin
data class MemberBalance(
    val userId: String,
    val name: String,
    val balance: Double   // positive = owed money, negative = owes money
)
```

#### `WalletUiState` Change
`WalletUiState.Success` replaces `balanceSummary: String` with `balanceRows: List<MemberBalance>`. `WalletFragment` renders this list via `BalanceSummaryAdapter` instead of setting text on a `TextView`.

#### Balance Card Redesign
- Replace `tvBalanceSummary` (`TextView`) in `cardBalance` with a `RecyclerView`
- `BalanceSummaryAdapter` displays one row per `MemberBalance` showing name + formatted balance
- Row colors:
  - Positive balance → `@color/success` ("owed money")
  - Negative balance → `@color/error` ("owes money")
  - Zero → `@color/text_tertiary`
- Rows with **negative balance** are tappable → opens `SettleUpBottomSheet` pre-filled with that member's data
- Rows with zero or positive balance are not tappable

#### SettleUpBottomSheet
`BottomSheetDialogFragment` with:

| Field | Behaviour |
|---|---|
| "From" | Read-only, pre-filled with selected member's name |
| "To" | Read-only, pre-filled with current user's name |
| Amount | Pre-filled with owed amount (absolute value), editable |
| Confirm button | Submits settlement |

On confirm: calls `WalletViewModel.settleUp(fromUserId, fromName, amount)`

`toUserId` and `toName` are not parameters — the "To" party is always the **current user** (`FirebaseAuth.currentUser.uid` / `currentUser.name`), resolved inside `settleUp()`. `participants` is set to `listOf(fromUserId, currentUser.uid)`.

#### Data Model — `Expense` gains `participants` field
Add `participants: List<String> = emptyList()` to the `Expense` data class.

- For normal expenses: `participants` is empty — balance calc splits among **all** members (existing behaviour, unchanged)
- For settlements: `participants = listOf(fromUserId, toUserId)` — balance calc splits **only** among those two users

This prevents settlements from incorrectly affecting uninvolved roommates in 3+ person apartments.

Settlements are stored in Firestore as:
```
category     = "Settlement"
description  = "{fromName} → {toName}"
amount       = <entered amount>
payerId      = fromUserId
payerName    = fromName
participants = [fromUserId, toUserId]
```

#### Balance Calculation Update
`WalletViewModel.calculateBalance()` changes its **return type from `String` to `List<MemberBalance>`**, and its **input signature from `nameMap: Map<String, String>` to `members: List<User>`** (using `User.userId` and `User.name` directly — the `nameMap` pre-build step is eliminated).

Instead of building a formatted string, it builds and returns a `MemberBalance` per member. The split logic gains one conditional:
```
val splitAmong = if (expense.participants.isEmpty()) members else members.filter { it.userId in expense.participants }
val perPersonShare = expense.amount / splitAmong.size
// Only credit/debit members in splitAmong
```

**`addExpense()` path and `_expenseSaveState` removal:** `WalletViewModel` currently has two LiveData fields — `_uiState` (expense list) and `_expenseSaveState` (save result) — observed by `WalletFragment` and `AddExpenseFragment` respectively. After this refactor:

- `_expenseSaveState: MutableLiveData<WalletUiState>` is **removed** (its type would conflict with the new `Success` shape).
- A new `val expenseSaved = MutableLiveData<Boolean>()` (or `SingleLiveEvent<Unit>`) is added to `WalletViewModel`. It emits `true` when `addExpense()` succeeds, `false` on error.
- `AddExpenseFragment` observes `expenseSaved` instead of `expenseSaveState` — navigates back on `true`, shows error toast on `false`.
- `WalletFragment` removes its `expenseSaveState` observer entirely; it already observes `_uiState`, which updates automatically when `loadExpenses()` is called after a save.
- `WalletFragment`'s Modified Files entry is updated to reflect this observer removal.

#### Display in Expense List
Settlements appear in the expense list like any other expense, with `category = "Settlement"` and the direction shown in the description.

#### `WalletRepository` (new file)
Extracted from `WalletViewModel` inline Firestore calls. Contains:
- `getExpenses(aptId): List<Expense>`
- `addExpense(aptId, expense: Expense)`
- `deleteExpense(aptId, expenseId)`
- `getApartmentMembers(aptId): List<User>`

`WalletViewModel` is refactored to delegate all Firestore operations to `WalletRepository`.

---

## Data Model Changes Summary

| Model | Change |
|---|---|
| `DashboardData` | Add `inviteCode: String` |
| `Task` | Add `createdBy: String` |
| `Expense` | Add `participants: List<String> = emptyList()` |
| `InventoryItem` | No changes |
| `User` | No changes |

---

## New Files

| File | Purpose |
|---|---|
| `WalletRepository.kt` | Firestore operations extracted from `WalletViewModel` |
| `SettleUpBottomSheet.kt` | Bottom sheet form for recording a settlement |
| `BalanceSummaryAdapter.kt` | RecyclerView adapter for per-member balance rows |
| `item_balance_row.xml` | Layout for a single balance row |
| `MemberBalance.kt` | Data class for a member's balance amount — placed in `com.example.homie.data.model` |

---

## Modified Files

| File | Change |
|---|---|
| `activity_login.xml` | Add "Forgot password?" link |
| `LoginActivity.kt` | Handle forgot password dialog + Firebase call |
| `fragment_dashboard.xml` | Add invite code row |
| `DashboardFragment.kt` | Bind invite code, clipboard copy |
| `DashboardData.kt` | Add `inviteCode: String` field |
| `DashboardUiState.kt` | Add `inviteCode: String` to `Success` state |
| `DashboardRepository.kt` | Pass `inviteCode` through from Apartment object |
| `Task.kt` | Add `createdBy: String` field |
| `Expense.kt` | Add `participants: List<String>` field |
| `fragment_add_task.xml` | Add "Assign to" spinner |
| `AddTaskFragment.kt` | Load members, wire spinner, pass assignee on save |
| `TasksViewModel.kt` | Add `loadMembers()`; update `addTask()` signature |
| `TasksRepository.kt` | Add `deleteTask()`, `getMembers()`; update `addTask()` signature to accept `assignedToId` + `assignedToName` and set `createdBy` |
| `InventoryRepository.kt` | Add `deleteInventoryItem()` |
| `WalletFragment.kt` | Attach swipe handler, wire `BalanceSummaryAdapter`, open bottom sheet |
| `WalletViewModel.kt` | Refactor to use `WalletRepository`; add `settleUp()`; update `calculateBalance()` for `participants`; expose `balanceRows: List<MemberBalance>` |
| `WalletUiState.kt` | Replace `balanceSummary: String` with `balanceRows: List<MemberBalance>` |
| `TasksFragment.kt` | Attach swipe handler |
| `InventoryFragment.kt` | Attach swipe handler |
| `fragment_wallet.xml` | Replace `tvBalanceSummary` with `RecyclerView` inside `cardBalance` |

---

## Implementation Order

Features must be implemented in this order to avoid dependency issues:

1. **G1** — Forgot Password (fully independent)
2. **G6** — Share Invite Code (fully independent)
3. **T1** — Assign Task (adds `Task.createdBy`, updates `addTask()` signature)
4. **W2+T2+I1** — Swipe to Delete (depends on `Task.createdBy` from T1; creates `WalletRepository`)
5. **W5** — Settle Up (depends on `WalletRepository` from step 4; adds `Expense.participants`)

---

## Out of Scope

- Partial settlements (settling a subset of the debt)
- Notifications when a settlement is recorded
- Editing existing expenses, tasks, or inventory items
- Any changes to the streak system
