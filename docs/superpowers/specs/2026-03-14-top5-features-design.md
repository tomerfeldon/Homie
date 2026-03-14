# Homie App — Top 5 Features Design Spec
**Date:** 2026-03-14
**Status:** Approved

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
- **Data flow:** `DashboardData` gains `inviteCode: String` field. `DashboardRepository` already fetches the full `Apartment` object — extract and pass `inviteCode` through. `DashboardUiState.Success` exposes it. `DashboardFragment` binds it on success.
- No new Firestore calls required.

---

## Feature 3 — Assign Task to Roommate (T1)

### Scope
`fragment_add_task.xml`, `AddTaskFragment.kt`, `TasksViewModel.kt`, `TasksRepository.kt`

### Design
- Add an "Assign to" `Spinner` (or `AutoCompleteTextView` dropdown) to the Add Task form, below the description field
- On fragment creation, load apartment members list via a new `TasksViewModel.loadMembers()` method that reuses the existing Firestore members-fetch pattern
- Spinner populates with member names; defaults to the current user (index 0)
- On save, `task.assignedTo` and `task.assignedToName` are set from the selected spinner item
- No `Task` model changes needed — `assignedTo` and `assignedToName` fields already exist

---

## Feature 4 — Swipe to Delete (W2 + T2 + I1)

### Scope
`WalletFragment.kt`, `TasksFragment.kt`, `InventoryFragment.kt`,
`ExpenseAdapter.kt`, `TasksAdapter.kt`, `InventoryAdapter.kt`,
`WalletRepository.kt` (new), `TasksRepository.kt`, `InventoryRepository.kt`

### Design

#### Gesture
- `ItemTouchHelper` configured for `LEFT` swipe, attached to each RecyclerView in the host Fragment
- Red delete background with a trash icon drawn via `onChildDraw` during swipe

#### Permission Check
Before showing confirmation, compare current user UID against the item's creator field:
| Screen | Creator field |
|---|---|
| Wallet | `expense.payerId` |
| Tasks | `task.assignedTo` (creator is assigner) |
| Inventory | `inventoryItem.addedBy` |

- If UID does not match: item snaps back, toast: "You can only delete items you created"
- If UID matches: show `MaterialAlertDialog` ("Delete this item? This cannot be undone")

#### On Confirm
- Item is deleted from Firestore
- On cancel: `notifyItemChanged(position)` to snap item back

#### Repository Methods (all one-liners)
- `WalletRepository.deleteExpense(aptId, expenseId)`
- `TasksRepository.deleteTask(aptId, taskId)`
- `InventoryRepository.deleteInventoryItem(aptId, itemId)`

Tasks and Inventory use real-time listeners — list updates automatically after delete. Wallet calls `loadExpenses()` after delete to refresh.

#### Note on Tasks
The current `task.assignedTo` stores the assignee, not the creator. After Feature T1 is implemented, a separate `createdBy` field should be used for the permission check. For now, `assignedTo` is used as a proxy (since tasks are currently always self-assigned).

---

## Feature 5 — Settle Up (W5)

### Scope
`fragment_wallet.xml`, `WalletFragment.kt`, `WalletViewModel.kt`,
new `SettleUpBottomSheet.kt`, new `item_balance_row.xml`,
new `BalanceSummaryAdapter.kt`, `WalletRepository.kt`

### Design

#### Balance Card Redesign
- Replace `tvBalanceSummary` (`TextView`) in `cardBalance` with a `RecyclerView`
- New `BalanceSummaryAdapter` displays one row per member showing name + balance
- Row colors:
  - Positive balance → `@color/success` ("owed money")
  - Negative balance → `@color/error` ("owes money")
  - Zero → `@color/text_tertiary`
- Rows with **negative balance** are tappable (clickable) → opens `SettleUpBottomSheet`
- Rows with zero or positive balance are not tappable

#### SettleUpBottomSheet
`BottomSheetDialogFragment` with:
| Field | Behaviour |
|---|---|
| "From" | Read-only, pre-filled with selected member's name |
| "To" | Read-only, pre-filled with current user's name |
| Amount | Pre-filled with owed amount (absolute value), editable |
| Confirm button | Submits settlement |

On confirm: calls `WalletViewModel.settleUp(fromUserId, fromName, toName, amount)`

#### Data Model — Settlement as Expense
Settlements are stored as regular `Expense` documents in Firestore:
```
category  = "Settlement"
description = "{fromName} → {toName}"
amount    = <entered amount>
payerId   = fromUserId   (the person paying their debt)
payerName = fromName
```
The existing balance calculation in `WalletViewModel.calculateBalance()` processes all expenses including settlements — no changes to the algorithm needed. The settlement payment correctly reduces the payer's negative balance.

#### Display in Expense List
Settlement items appear in the expense list like any other expense. `ExpenseAdapter` renders them with `category = "Settlement"` and the description showing the direction of payment.

---

## Data Model Changes Summary

| Model | Change |
|---|---|
| `DashboardData` | Add `inviteCode: String` |
| `Task` | No changes (existing fields sufficient) |
| `Expense` | No changes (Settlement reuses existing schema) |
| `InventoryItem` | No changes |
| `User` | No changes |

---

## New Files

| File | Purpose |
|---|---|
| `SettleUpBottomSheet.kt` | Bottom sheet form for recording a settlement |
| `BalanceSummaryAdapter.kt` | RecyclerView adapter for per-member balance rows |
| `item_balance_row.xml` | Layout for a single balance row |

---

## Modified Files

| File | Change |
|---|---|
| `activity_login.xml` | Add "Forgot password?" link |
| `LoginActivity.kt` | Handle forgot password dialog + Firebase call |
| `fragment_dashboard.xml` | Add invite code row |
| `DashboardFragment.kt` | Bind invite code, clipboard copy |
| `DashboardData.kt` | Add `inviteCode` field |
| `DashboardUiState.kt` | Expose `inviteCode` in Success state |
| `DashboardRepository.kt` | Pass `inviteCode` through from Apartment |
| `fragment_add_task.xml` | Add "Assign to" spinner |
| `AddTaskFragment.kt` | Load members, wire spinner, pass assignee on save |
| `TasksViewModel.kt` | Add `loadMembers()` |
| `TasksRepository.kt` | Add `deleteTask()`, `getMembers()` |
| `InventoryRepository.kt` | Add `deleteInventoryItem()` |
| `WalletFragment.kt` | Attach swipe handler, wire BalanceSummaryAdapter, open bottom sheet |
| `WalletViewModel.kt` | Add `settleUp()`, expose balance rows as list |
| `TasksFragment.kt` | Attach swipe handler |
| `InventoryFragment.kt` | Attach swipe handler |
| `fragment_wallet.xml` | Replace `tvBalanceSummary` with RecyclerView |

---

## Out of Scope

- Partial settlements (settling a subset of the debt)
- Notifications when a settlement is recorded
- Editing existing expenses, tasks, or inventory items
- Any changes to the streak system
