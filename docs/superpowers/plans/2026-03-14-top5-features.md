# Top 5 Features Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add forgot-password, invite-code sharing, task assignment, swipe-to-delete, and settle-up to the Homie Android roommate app.

**Architecture:** MVVM with Firebase Firestore; each feature touches its own ViewModel/Repository/Fragment layer without cross-feature coupling. Features 3 and 4 share the `Task.createdBy` field, so Task changes must land before delete is wired.

**Tech Stack:** Kotlin, Firebase Auth/Firestore/Storage, Material Design 3, Navigation Component, View Binding, RecyclerView ListAdapter, ItemTouchHelper

---

## Chunk 1: G1 — Forgot Password + G6 — Share Invite Code

### Task 1: Add "Forgot password?" link to login screen

**Files:**
- Modify: `app/src/main/res/layout/activity_login.xml`

- [ ] **Step 1: Add the link below `btnLogin` inside the `LinearLayoutCompat`**

  Add this block immediately after the closing `</com.google.android.material.button.MaterialButton>` tag for `btnLogin` and before `<LinearLayout android:id="@+id/llRegister"`:

  ```xml
  <TextView
      android:id="@+id/tvForgotPassword"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_gravity="center"
      android:layout_marginTop="8dp"
      android:text="Forgot password?"
      android:textColor="@color/app_color"
      android:textSize="14sp"
      android:background="?attr/selectableItemBackground"
      android:padding="8dp" />
  ```

- [ ] **Step 2: Build to verify no XML errors**

  ```bash
  cd /mnt/c/Users/tomer/Desktop/Homie && ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 2: Wire forgot-password dialog in LoginActivity

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/auth/LoginActivity.kt`

- [ ] **Step 1: Add click listener in `onCreate` after the existing listeners**

  In `onCreate`, after `binding.llRegister.setOnClickListener { ... }`, add:

  ```kotlin
  binding.tvForgotPassword.setOnClickListener {
      showForgotPasswordDialog()
  }
  ```

- [ ] **Step 2: Add the dialog function at the bottom of the class**

  ```kotlin
  private fun showForgotPasswordDialog() {
      val emailInput = com.google.android.material.textfield.TextInputEditText(this)
      emailInput.hint = "Email"
      emailInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
      val container = android.widget.FrameLayout(this).apply {
          val padding = resources.getDimensionPixelSize(
              com.google.android.material.R.dimen.m3_alert_dialog_action_spacing
          )
          setPadding(padding, 0, padding, 0)
          addView(emailInput)
      }

      com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
          .setTitle("Reset Password")
          .setMessage("Enter your email address and we'll send you a reset link.")
          .setView(container)
          .setPositiveButton("Send") { _, _ ->
              val email = emailInput.text.toString().trim()
              if (email.isEmpty()) {
                  Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
                  return@setPositiveButton
              }
              auth.sendPasswordResetEmail(email)
                  .addOnSuccessListener {
                      Toast.makeText(this, "Check your email for a reset link", Toast.LENGTH_LONG).show()
                  }
                  .addOnFailureListener {
                      Toast.makeText(this, it.message ?: "Failed to send email", Toast.LENGTH_LONG).show()
                  }
          }
          .setNegativeButton("Cancel", null)
          .show()
  }
  ```

- [ ] **Step 3: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Manual test**
  - Launch app → tap "Forgot password?" → dialog appears with email field
  - Enter valid email → "Check your email..." toast appears
  - Enter invalid/unregistered email → Firebase error toast appears
  - Tap Cancel → dialog dismisses, no action

- [ ] **Step 5: Commit**

  ```bash
  git add app/src/main/res/layout/activity_login.xml \
          app/src/main/java/com/example/homie/ui/auth/LoginActivity.kt
  git commit -m "feat: add forgot password dialog to login screen"
  ```

---

### Task 3: Update DashboardData and DashboardUiState for invite code

**Files:**
- Modify: `app/src/main/java/com/example/homie/data/model/DashboardData.kt`
- Modify: `app/src/main/java/com/example/homie/ui/main/dashboard/DashboardUiState.kt`

- [ ] **Step 1: Add `inviteCode` to DashboardData**

  Replace the entire file contents with:

  ```kotlin
  package com.example.homie.data.model

  data class DashboardData(
      val apartmentName: String,
      val members: List<User>,
      val urgentTasks: List<Task>,
      val debtText: String,
      val inviteCode: String
  )
  ```

- [ ] **Step 2: Add `inviteCode` to DashboardUiState.Success**

  Replace the entire file contents with:

  ```kotlin
  package com.example.homie.ui.main.dashboard

  import com.example.homie.data.model.Task
  import com.example.homie.data.model.User

  sealed class DashboardUiState {
      object Loading : DashboardUiState()
      data class Success(
          val apartmentName: String,
          val members: List<User>,
          val urgentTasks: List<Task>,
          val debtText: String,
          val inviteCode: String
      ) : DashboardUiState()
      data class Error(val message: String) : DashboardUiState()
  }
  ```

- [ ] **Step 3: Build — expect compile errors in DashboardRepository and DashboardViewModel (constructor mismatch)**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: compile errors mentioning `DashboardData` and `DashboardUiState.Success` — these are fixed in the next tasks.

---

### Task 4: Pass inviteCode through DashboardRepository and DashboardViewModel

**Files:**
- Modify: `app/src/main/java/com/example/homie/data/repository/DashboardRepository.kt`
- Modify: `app/src/main/java/com/example/homie/ui/main/dashboard/DashboardViewModel.kt`

- [ ] **Step 1: Update `DashboardData` constructor call in `DashboardRepository.getFullDashboardData()`**

  Find the `Result.success(DashboardData(...))` call (around line 78) and replace it:

  ```kotlin
  Result.success(
      DashboardData(
          apartment.name,
          members,
          urgentTasks,
          debtText,
          apartment.inviteCode
      )
  )
  ```

- [ ] **Step 2: Update `DashboardUiState.Success` constructor call in `DashboardViewModel.loadDashboard()`**

  Find the `_uiState.value = DashboardUiState.Success(...)` call and replace it:

  ```kotlin
  _uiState.value = DashboardUiState.Success(
      apartmentName = it.apartmentName,
      members = it.members,
      urgentTasks = it.urgentTasks,
      debtText = it.debtText,
      inviteCode = it.inviteCode
  )
  ```

- [ ] **Step 3: Build — should now only fail in DashboardFragment (binding.tvInviteCode not yet in layout)**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL` (DashboardFragment doesn't reference the new field yet)

---

### Task 5: Add copy icon drawable + invite code row to Dashboard layout

**Files:**
- Create: `app/src/main/res/drawable/baseline_content_copy_24.xml`
- Modify: `app/src/main/res/layout/fragment_dashboard.xml`

- [ ] **Step 1: Create the copy icon vector drawable**

  Create `app/src/main/res/drawable/baseline_content_copy_24.xml`:

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:viewportWidth="24"
      android:viewportHeight="24">
    <path
        android:fillColor="@android:color/black"
        android:pathData="M16,1H4C2.9,1 2,1.9 2,3v14h2V3h12V1zM19,5H8C6.9,5 6,5.9 6,7v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7C22,5.9 21.1,5 19,5zM19,21H8V7h11V21z"/>
  </vector>
  ```

- [ ] **Step 2: Add invite code row to `fragment_dashboard.xml`**

  In `fragment_dashboard.xml`, find the `<!-- Apartment Name -->` TextView (`tvApartmentName`) and add the following LinearLayout immediately after it (before `<!-- Members Section -->`):

  ```xml
  <!-- Invite Code Row -->
  <LinearLayout
      android:id="@+id/llInviteCode"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_marginTop="@dimen/spacing_xs"
      android:gravity="center_vertical"
      android:orientation="horizontal">

      <TextView
          android:id="@+id/tvInviteCode"
          android:layout_width="0dp"
          android:layout_height="wrap_content"
          android:layout_weight="1"
          android:text="Invite code: ------"
          android:textAppearance="@style/TextAppearance.Homie.BodyMedium"
          android:textColor="@color/text_secondary" />

      <ImageView
          android:id="@+id/ivCopyCode"
          android:layout_width="36dp"
          android:layout_height="36dp"
          android:padding="6dp"
          android:src="@drawable/baseline_content_copy_24"
          android:background="?attr/selectableItemBackgroundBorderless"
          app:tint="@color/primary" />

  </LinearLayout>
  ```

- [ ] **Step 3: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 6: Wire invite code in DashboardFragment

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/dashboard/DashboardFragment.kt`

- [ ] **Step 1: Add a field to hold the current invite code**

  Add at the top of the class, after `private lateinit var progressDialog: ProgressDialog`:

  ```kotlin
  private var currentInviteCode = ""
  ```

- [ ] **Step 2: Bind the invite code in the `Success` branch of `observeUiState()`**

  Inside `is DashboardUiState.Success -> { ... }`, after `tasksAdapter.updateData(state.urgentTasks)`, add:

  ```kotlin
  currentInviteCode = state.inviteCode
  binding.tvInviteCode.text = "Invite code: ${state.inviteCode}"
  ```

- [ ] **Step 3: Set up the copy button in `onViewCreated`**

  After `viewModel.loadDashboard()`, add:

  ```kotlin
  binding.ivCopyCode.setOnClickListener {
      val clipboard = requireContext()
          .getSystemService(android.content.Context.CLIPBOARD_SERVICE)
              as android.content.ClipboardManager
      val clip = android.content.ClipData.newPlainText("Invite Code", currentInviteCode)
      clipboard.setPrimaryClip(clip)
      Toast.makeText(requireContext(), "Invite code copied!", Toast.LENGTH_SHORT).show()
  }
  ```

- [ ] **Step 4: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Manual test**
  - Open Dashboard → invite code row shows "Invite code: XXXXXX"
  - Tap copy icon → toast "Invite code copied!" → paste elsewhere confirms the code

- [ ] **Step 6: Commit**

  ```bash
  git add app/src/main/java/com/example/homie/data/model/DashboardData.kt \
          app/src/main/java/com/example/homie/ui/main/dashboard/DashboardUiState.kt \
          app/src/main/java/com/example/homie/data/repository/DashboardRepository.kt \
          app/src/main/java/com/example/homie/ui/main/dashboard/DashboardViewModel.kt \
          app/src/main/res/drawable/baseline_content_copy_24.xml \
          app/src/main/res/layout/fragment_dashboard.xml \
          app/src/main/java/com/example/homie/ui/main/dashboard/DashboardFragment.kt
  git commit -m "feat: show and copy apartment invite code on dashboard"
  ```

---

## Chunk 2: T1 — Assign Task to Roommate

### Task 7: Add `createdBy` field to Task model

**Files:**
- Modify: `app/src/main/java/com/example/homie/data/model/Task.kt`

- [ ] **Step 1: Add the field**

  Replace the entire file:

  ```kotlin
  package com.example.homie.data.model

  data class Task(
      var id: String = "",
      var title: String = "",
      var description: String = "",
      var assignedTo: String = "",
      var assignedToName: String = "",
      var createdBy: String = "",
      var completed: Boolean = false,
      var timestamp: Long = System.currentTimeMillis()
  )
  ```

- [ ] **Step 2: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL` (new field has a default, so no constructor breaks)

---

### Task 8: Add `getMembers()` and `deleteTask()` to TasksRepository; update `addTask()` signature

**Files:**
- Modify: `app/src/main/java/com/example/homie/data/repository/TasksRepository.kt`

- [ ] **Step 1: Add the import for User**

  At the top of `TasksRepository.kt`, add:

  ```kotlin
  import com.example.homie.data.model.User
  ```

- [ ] **Step 2: Add `getMembers()` method**

  Add after `getApartmentId()`:

  ```kotlin
  suspend fun getMembers(apartmentId: String): List<User> {
      val aptDoc = firestore.collection("apartments")
          .document(apartmentId)
          .get()
          .await()
      val memberIds = aptDoc.get("members") as? List<String> ?: return emptyList()
      return memberIds.mapNotNull { uid ->
          firestore.collection("users")
              .document(uid)
              .get()
              .await()
              .toObject(User::class.java)
      }
  }
  ```

- [ ] **Step 3: Update `addTask()` signature to accept assignee + set `createdBy`**

  Replace the existing `addTask()` function:

  ```kotlin
  suspend fun addTask(
      apartmentId: String,
      title: String,
      description: String,
      assignedToId: String,
      assignedToName: String
  ) {
      val user = auth.currentUser ?: return
      val taskId = UUID.randomUUID().toString()
      val task = Task(
          id = taskId,
          title = title,
          description = description,
          assignedTo = assignedToId,
          assignedToName = assignedToName,
          createdBy = user.uid,
          completed = false,
          timestamp = System.currentTimeMillis()
      )
      firestore.collection("apartments")
          .document(apartmentId)
          .collection("tasks")
          .document(taskId)
          .set(task)
          .await()
  }
  ```

- [ ] **Step 4: Add `deleteTask()` method**

  Add after `addTask()`:

  ```kotlin
  suspend fun deleteTask(apartmentId: String, taskId: String) {
      firestore.collection("apartments")
          .document(apartmentId)
          .collection("tasks")
          .document(taskId)
          .delete()
          .await()
  }
  ```

- [ ] **Step 5: Build — expect compile error in TasksViewModel (addTask call signature mismatch)**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: error in `TasksViewModel.addTask()` — fixed next.

---

### Task 9: Update TasksViewModel — add `loadMembers()`, `deleteTask()`, update `addTask()`

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/tasks/TasksViewModel.kt`

- [ ] **Step 1: Add User import and new LiveData field for members**

  Add import at top:

  ```kotlin
  import com.example.homie.data.model.User
  ```

  Add new LiveData field after `_addTaskState`:

  ```kotlin
  private val _membersState = MutableLiveData<List<User>>()
  val membersState: LiveData<List<User>> = _membersState
  ```

- [ ] **Step 2: Add `loadMembers()` function**

  Add after `loadTasks()`:

  ```kotlin
  fun loadMembers() {
      viewModelScope.launch {
          try {
              if (ensureApartmentLoaded()) {
                  val members = repository.getMembers(apartmentId!!)
                  _membersState.value = members
              }
          } catch (e: Exception) {
              // fail silently; spinner will remain empty
          }
      }
  }
  ```

- [ ] **Step 3: Update `addTask()` to accept and forward assignee parameters**

  Replace the existing `addTask()` function:

  ```kotlin
  fun addTask(title: String, description: String, assignedToId: String, assignedToName: String) {
      viewModelScope.launch {
          _addTaskState.value = TaskUiState.Loading
          try {
              if (ensureApartmentLoaded()) {
                  repository.addTask(apartmentId!!, title, description, assignedToId, assignedToName)
                  _addTaskState.value = TaskUiState.Success(Unit)
              } else {
                  _addTaskState.value = TaskUiState.Error("Apartment not found")
              }
          } catch (e: Exception) {
              _addTaskState.value = TaskUiState.Error(e.message ?: "Failed to add task")
          }
      }
  }
  ```

- [ ] **Step 4: Add `deleteTask()` function**

  Add after `completeTask()`:

  ```kotlin
  fun deleteTask(task: Task) {
      viewModelScope.launch {
          try {
              apartmentId?.let { repository.deleteTask(it, task.id) }
          } catch (e: Exception) {
              _tasksState.value = TaskUiState.Error(e.message ?: "Failed to delete task")
          }
      }
  }
  ```

- [ ] **Step 5: Build — expect compile error in AddTaskFragment (addTask call mismatch)**

  ```bash
  ./gradlew assembleDebug
  ```

---

### Task 10: Add "Assign to" spinner to Add Task form layout

**Files:**
- Modify: `app/src/main/res/layout/fragment_add_task.xml`

- [ ] **Step 1: Add spinner and label between description field and save button**

  In `fragment_add_task.xml`, inside the `<LinearLayout android:orientation="vertical">` in the ScrollView, add the following block between the description `TextInputLayout` and the save button:

  ```xml
  <TextView
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_marginTop="16dp"
      android:text="Assign to"
      android:textColor="@color/text_secondary"
      android:textSize="14sp" />

  <Spinner
      android:id="@+id/spinnerAssignee"
      android:layout_width="match_parent"
      android:layout_height="48dp"
      android:layout_marginTop="4dp" />
  ```

  Also fix the typo in the header: change `android:text="Add Taask"` to `android:text="Add Task"`.

- [ ] **Step 2: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 11: Wire spinner and updated addTask() in AddTaskFragment

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/tasks/AddTaskFragment.kt`

- [ ] **Step 1: Add a field to hold the loaded members list**

  Add at top of class:

  ```kotlin
  private var membersList: List<com.example.homie.data.model.User> = emptyList()
  ```

- [ ] **Step 2: Call `loadMembers()` in `onViewCreated` and observe result**

  At the end of `onViewCreated`, add:

  ```kotlin
  viewModel.loadMembers()
  ```

  In `observeViewModel()`, add a second observer:

  ```kotlin
  viewModel.membersState.observe(viewLifecycleOwner) { members ->
      membersList = members
      val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
      val sortedMembers = members.sortedWith(
          compareByDescending { it.userId == currentUid }
      )
      membersList = sortedMembers
      val names = sortedMembers.map { it.name.ifEmpty { it.email } }
      val spinnerAdapter = android.widget.ArrayAdapter(
          requireContext(),
          android.R.layout.simple_spinner_dropdown_item,
          names
      )
      binding.spinnerAssignee.adapter = spinnerAdapter
  }
  ```

- [ ] **Step 3: Update the save button click to pass assignee**

  Replace the `binding.btnSaveTask.setOnClickListener` block:

  ```kotlin
  binding.btnSaveTask.setOnClickListener {
      val title = binding.etTitle.text.toString().trim()
      val description = binding.etDescription.text.toString().trim()

      if (title.isEmpty()) {
          Toast.makeText(requireContext(), "Enter title", Toast.LENGTH_SHORT).show()
          return@setOnClickListener
      }

      val selectedIndex = binding.spinnerAssignee.selectedItemPosition
      if (membersList.isEmpty() || selectedIndex < 0) {
          Toast.makeText(requireContext(), "Please wait, loading members...", Toast.LENGTH_SHORT).show()
          return@setOnClickListener
      }

      val selectedMember = membersList[selectedIndex]
      viewModel.addTask(title, description, selectedMember.userId, selectedMember.name)
  }
  ```

- [ ] **Step 4: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Manual test**
  - Open Add Task → spinner shows all roommates, current user first
  - Select a different roommate → save → task appears assigned to that person in Tasks list
  - New tasks have `createdBy` field populated in Firestore (verify in Firebase console)

- [ ] **Step 6: Commit**

  ```bash
  git add app/src/main/java/com/example/homie/data/model/Task.kt \
          app/src/main/java/com/example/homie/data/repository/TasksRepository.kt \
          app/src/main/java/com/example/homie/ui/main/tasks/TasksViewModel.kt \
          app/src/main/res/layout/fragment_add_task.xml \
          app/src/main/java/com/example/homie/ui/main/tasks/AddTaskFragment.kt
  git commit -m "feat: allow assigning tasks to any roommate; add createdBy field to Task"
  ```

---

## Chunk 3: W2+T2+I1 — Swipe to Delete

### Task 12: Add delete drawable + create WalletRepository with `deleteExpense()`

**Files:**
- Create: `app/src/main/res/drawable/baseline_delete_24.xml`
- Create: `app/src/main/java/com/example/homie/data/repository/WalletRepository.kt`

- [ ] **Step 1: Create trash icon vector drawable**

  Create `app/src/main/res/drawable/baseline_delete_24.xml`:

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:viewportWidth="24"
      android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z"/>
  </vector>
  ```

- [ ] **Step 2: Create WalletRepository with `deleteExpense()`**

  Create `app/src/main/java/com/example/homie/data/repository/WalletRepository.kt`:

  ```kotlin
  package com.example.homie.data.repository

  import com.google.firebase.firestore.FirebaseFirestore
  import kotlinx.coroutines.tasks.await

  class WalletRepository {

      private val firestore = FirebaseFirestore.getInstance()

      suspend fun deleteExpense(aptId: String, expenseId: String) {
          firestore.collection("apartments")
              .document(aptId)
              .collection("expenses")
              .document(expenseId)
              .delete()
              .await()
      }
  }
  ```

- [ ] **Step 3: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 13: Add `deleteExpense()` to WalletViewModel + `deleteInventoryItem()` to InventoryRepository

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/wallet/WalletViewModel.kt`
- Modify: `app/src/main/java/com/example/homie/data/repository/InventoryRepository.kt`
- Modify: `app/src/main/java/com/example/homie/ui/main/inventory/InventoryViewModel.kt`

- [ ] **Step 1: Add `WalletRepository` to `WalletViewModel` and add `deleteExpense()`**

  At the top of `WalletViewModel`, add a repository field after the existing Firebase fields:

  ```kotlin
  private val walletRepository = WalletRepository()
  ```

  Add this import:

  ```kotlin
  import com.example.homie.data.repository.WalletRepository
  ```

  Add the `deleteExpense()` function after `addExpense()`:

  ```kotlin
  fun deleteExpense(expenseId: String) {
      viewModelScope.launch {
          try {
              val currentUser = auth.currentUser ?: throw Exception("User not logged in")
              val userDoc = firestore.collection("users")
                  .document(currentUser.uid).get().await()
              val aptId = userDoc.getString("apartmentId")
                  ?: throw Exception("No apartment found")
              walletRepository.deleteExpense(aptId, expenseId)
              loadExpenses()
          } catch (e: Exception) {
              _uiState.value = WalletUiState.Error(e.message ?: "Failed to delete")
          }
      }
  }
  ```

- [ ] **Step 2: Add `deleteInventoryItem()` to `InventoryRepository`**

  Add at the end of `InventoryRepository`:

  ```kotlin
  suspend fun deleteInventoryItem(apartmentId: String, itemId: String) {
      firestore.collection("apartments")
          .document(apartmentId)
          .collection("inventory")
          .document(itemId)
          .delete()
          .await()
  }
  ```

- [ ] **Step 3: Add `deleteItem()` to `InventoryViewModel`**

  Add at the end of `InventoryViewModel`:

  ```kotlin
  fun deleteItem(item: InventoryItem) {
      viewModelScope.launch {
          try {
              apartmentId?.let { repository.deleteInventoryItem(it, item.id) }
          } catch (e: Exception) {
              _inventoryState.value = InventoryUiState.Error(e.message ?: "Failed to delete")
          }
      }
  }
  ```

- [ ] **Step 4: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 14: Add swipe-to-delete to WalletFragment

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/wallet/WalletFragment.kt`

- [ ] **Step 1: Add required imports at top of file**

  ```kotlin
  import android.graphics.Canvas
  import android.graphics.Color
  import android.graphics.drawable.ColorDrawable
  import androidx.core.content.ContextCompat
  import androidx.recyclerview.widget.ItemTouchHelper
  import androidx.recyclerview.widget.RecyclerView
  import com.example.homie.R
  import com.google.android.material.dialog.MaterialAlertDialogBuilder
  import com.google.firebase.auth.FirebaseAuth
  ```

- [ ] **Step 2: Add `setupSwipeToDelete()` function to the class**

  ```kotlin
  private fun setupSwipeToDelete() {
      val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
          override fun onMove(
              rv: RecyclerView,
              vh: RecyclerView.ViewHolder,
              t: RecyclerView.ViewHolder
          ) = false

          override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
              val position = viewHolder.adapterPosition
              val expense = adapter.currentList[position]
              val currentUid = FirebaseAuth.getInstance().currentUser?.uid

              if (expense.payerId != currentUid) {
                  adapter.notifyItemChanged(position)
                  Toast.makeText(
                      requireContext(),
                      "You can only delete items you created",
                      Toast.LENGTH_SHORT
                  ).show()
                  return
              }

              MaterialAlertDialogBuilder(requireContext())
                  .setTitle("Delete expense?")
                  .setMessage("This cannot be undone.")
                  .setPositiveButton("Delete") { _, _ ->
                      viewModel.deleteExpense(expense.id)
                  }
                  .setNegativeButton("Cancel") { _, _ ->
                      adapter.notifyItemChanged(position)
                  }
                  .setOnCancelListener {
                      adapter.notifyItemChanged(position)
                  }
                  .show()
          }

          override fun onChildDraw(
              c: Canvas,
              recyclerView: RecyclerView,
              viewHolder: RecyclerView.ViewHolder,
              dX: Float, dY: Float,
              actionState: Int,
              isCurrentlyActive: Boolean
          ) {
              val itemView = viewHolder.itemView
              val background = ColorDrawable(Color.parseColor("#E53935"))
              background.setBounds(
                  itemView.right + dX.toInt(), itemView.top,
                  itemView.right, itemView.bottom
              )
              background.draw(c)
              ContextCompat.getDrawable(recyclerView.context, R.drawable.baseline_delete_24)
                  ?.let { icon ->
                      val margin = (itemView.height - icon.intrinsicHeight) / 2
                      val top = itemView.top + margin
                      val right = itemView.right - margin
                      icon.setBounds(right - icon.intrinsicWidth, top, right, top + icon.intrinsicHeight)
                      icon.setTint(Color.WHITE)
                      icon.draw(c)
                  }
              super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
          }
      }
      ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvExpenses)
  }
  ```

- [ ] **Step 3: Call `setupSwipeToDelete()` in `onViewCreated`**

  At the end of `onViewCreated`, after `viewModel.loadExpenses()`, add:

  ```kotlin
  setupSwipeToDelete()
  ```

- [ ] **Step 4: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 15: Add swipe-to-delete to TasksFragment

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/tasks/TasksFragment.kt`

- [ ] **Step 1: Add required imports**

  ```kotlin
  import android.graphics.Canvas
  import android.graphics.Color
  import android.graphics.drawable.ColorDrawable
  import androidx.core.content.ContextCompat
  import androidx.recyclerview.widget.ItemTouchHelper
  import androidx.recyclerview.widget.RecyclerView
  import com.example.homie.R
  import com.google.android.material.dialog.MaterialAlertDialogBuilder
  import com.google.firebase.auth.FirebaseAuth
  ```

- [ ] **Step 2: Add `setupSwipeToDelete()` function**

  ```kotlin
  private fun setupSwipeToDelete() {
      val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
          override fun onMove(
              rv: RecyclerView,
              vh: RecyclerView.ViewHolder,
              t: RecyclerView.ViewHolder
          ) = false

          override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
              val position = viewHolder.adapterPosition
              val task = adapter.currentList[position]
              val currentUid = FirebaseAuth.getInstance().currentUser?.uid

              if (task.createdBy != currentUid) {
                  adapter.notifyItemChanged(position)
                  Toast.makeText(
                      requireContext(),
                      "You can only delete tasks you created",
                      Toast.LENGTH_SHORT
                  ).show()
                  return
              }

              MaterialAlertDialogBuilder(requireContext())
                  .setTitle("Delete task?")
                  .setMessage("This cannot be undone.")
                  .setPositiveButton("Delete") { _, _ ->
                      viewModel.deleteTask(task)
                  }
                  .setNegativeButton("Cancel") { _, _ ->
                      adapter.notifyItemChanged(position)
                  }
                  .setOnCancelListener {
                      adapter.notifyItemChanged(position)
                  }
                  .show()
          }

          override fun onChildDraw(
              c: Canvas,
              recyclerView: RecyclerView,
              viewHolder: RecyclerView.ViewHolder,
              dX: Float, dY: Float,
              actionState: Int,
              isCurrentlyActive: Boolean
          ) {
              val itemView = viewHolder.itemView
              val background = ColorDrawable(Color.parseColor("#E53935"))
              background.setBounds(
                  itemView.right + dX.toInt(), itemView.top,
                  itemView.right, itemView.bottom
              )
              background.draw(c)
              ContextCompat.getDrawable(recyclerView.context, R.drawable.baseline_delete_24)
                  ?.let { icon ->
                      val margin = (itemView.height - icon.intrinsicHeight) / 2
                      val top = itemView.top + margin
                      val right = itemView.right - margin
                      icon.setBounds(right - icon.intrinsicWidth, top, right, top + icon.intrinsicHeight)
                      icon.setTint(Color.WHITE)
                      icon.draw(c)
                  }
              super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
          }
      }
      ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvTasks)
  }
  ```

- [ ] **Step 3: Call `setupSwipeToDelete()` in `onViewCreated`**

  After `binding.fabAddTask.setOnClickListener { ... }`, add:

  ```kotlin
  setupSwipeToDelete()
  ```

- [ ] **Step 4: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 16: Add swipe-to-delete to InventoryFragment

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/inventory/InventoryFragment.kt`

- [ ] **Step 1: Add required imports**

  ```kotlin
  import android.graphics.Canvas
  import android.graphics.Color
  import android.graphics.drawable.ColorDrawable
  import androidx.core.content.ContextCompat
  import androidx.recyclerview.widget.ItemTouchHelper
  import androidx.recyclerview.widget.RecyclerView
  import com.example.homie.R
  import com.google.android.material.dialog.MaterialAlertDialogBuilder
  import com.google.firebase.auth.FirebaseAuth
  ```

- [ ] **Step 2: Add `setupSwipeToDelete()` function**

  ```kotlin
  private fun setupSwipeToDelete() {
      val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
          override fun onMove(
              rv: RecyclerView,
              vh: RecyclerView.ViewHolder,
              t: RecyclerView.ViewHolder
          ) = false

          override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
              val position = viewHolder.adapterPosition
              val item = adapter.currentList[position]
              val currentUid = FirebaseAuth.getInstance().currentUser?.uid

              if (item.addedBy != currentUid) {
                  adapter.notifyItemChanged(position)
                  Toast.makeText(
                      requireContext(),
                      "You can only delete items you added",
                      Toast.LENGTH_SHORT
                  ).show()
                  return
              }

              MaterialAlertDialogBuilder(requireContext())
                  .setTitle("Delete item?")
                  .setMessage("This cannot be undone.")
                  .setPositiveButton("Delete") { _, _ ->
                      viewModel.deleteItem(item)
                  }
                  .setNegativeButton("Cancel") { _, _ ->
                      adapter.notifyItemChanged(position)
                  }
                  .setOnCancelListener {
                      adapter.notifyItemChanged(position)
                  }
                  .show()
          }

          override fun onChildDraw(
              c: Canvas,
              recyclerView: RecyclerView,
              viewHolder: RecyclerView.ViewHolder,
              dX: Float, dY: Float,
              actionState: Int,
              isCurrentlyActive: Boolean
          ) {
              val itemView = viewHolder.itemView
              val background = ColorDrawable(Color.parseColor("#E53935"))
              background.setBounds(
                  itemView.right + dX.toInt(), itemView.top,
                  itemView.right, itemView.bottom
              )
              background.draw(c)
              ContextCompat.getDrawable(recyclerView.context, R.drawable.baseline_delete_24)
                  ?.let { icon ->
                      val margin = (itemView.height - icon.intrinsicHeight) / 2
                      val top = itemView.top + margin
                      val right = itemView.right - margin
                      icon.setBounds(right - icon.intrinsicWidth, top, right, top + icon.intrinsicHeight)
                      icon.setTint(Color.WHITE)
                      icon.draw(c)
                  }
              super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
          }
      }
      ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvInventory)
  }
  ```

- [ ] **Step 3: Call `setupSwipeToDelete()` in `onViewCreated`**

  After `viewModel.loadInventory()`, add:

  ```kotlin
  setupSwipeToDelete()
  ```

- [ ] **Step 4: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Manual test**
  - Wallet: swipe left on own expense → red background + trash icon → delete dialog → confirm → item removed and balance recalculates
  - Wallet: swipe left on another user's expense → snaps back + toast
  - Tasks: same pattern using `task.createdBy`
  - Inventory: same pattern using `item.addedBy`
  - Cancel on dialog → item snaps back

- [ ] **Step 6: Commit**

  ```bash
  git add app/src/main/res/drawable/baseline_delete_24.xml \
          app/src/main/java/com/example/homie/data/repository/WalletRepository.kt \
          app/src/main/java/com/example/homie/ui/main/wallet/WalletViewModel.kt \
          app/src/main/java/com/example/homie/data/repository/InventoryRepository.kt \
          app/src/main/java/com/example/homie/ui/main/inventory/InventoryViewModel.kt \
          app/src/main/java/com/example/homie/ui/main/wallet/WalletFragment.kt \
          app/src/main/java/com/example/homie/ui/main/tasks/TasksFragment.kt \
          app/src/main/java/com/example/homie/ui/main/inventory/InventoryFragment.kt
  git commit -m "feat: swipe-to-delete on expenses, tasks, and inventory items"
  ```

---

## Chunk 4: W5 — Settle Up

### Task 17: Add `MemberBalance` model and `participants` field to `Expense`

**Files:**
- Create: `app/src/main/java/com/example/homie/data/model/MemberBalance.kt`
- Modify: `app/src/main/java/com/example/homie/data/model/Expense.kt`

- [ ] **Step 1: Create `MemberBalance` data class**

  Create `app/src/main/java/com/example/homie/data/model/MemberBalance.kt`:

  ```kotlin
  package com.example.homie.data.model

  data class MemberBalance(
      val userId: String,
      val name: String,
      val balance: Double  // positive = owed money, negative = owes money
  )
  ```

- [ ] **Step 2: Add `participants` field to `Expense`**

  Replace the entire `Expense.kt`:

  ```kotlin
  package com.example.homie.data.model

  data class Expense(
      val id: String = "",
      val amount: Double = 0.0,
      val category: String = "",
      val description: String = "",
      val payerId: String = "",
      val payerName: String = "",
      val receiptUrl: String? = null,
      val participants: List<String> = emptyList(),
      val timestamp: Long = System.currentTimeMillis()
  )
  ```

- [ ] **Step 3: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL` (new field has default, no breaks)

---

### Task 18: Update `WalletUiState` to use `List<MemberBalance>`

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/wallet/WalletUiState.kt`

- [ ] **Step 1: Replace `balanceSummary: String` with `balanceRows: List<MemberBalance>`**

  Replace the entire file:

  ```kotlin
  package com.example.homie.ui.main.wallet

  import com.example.homie.data.model.Expense
  import com.example.homie.data.model.MemberBalance

  sealed class WalletUiState {
      object Loading : WalletUiState()
      data class Success(
          val expenses: List<Expense>,
          val balanceRows: List<MemberBalance>
      ) : WalletUiState()
      data class Error(val message: String) : WalletUiState()
  }
  ```

- [ ] **Step 2: Build — expect compile errors in WalletViewModel and WalletFragment**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: errors about `balanceSummary` — fixed in the next tasks.

---

### Task 19: Expand WalletRepository; refactor WalletViewModel

**Files:**
- Modify: `app/src/main/java/com/example/homie/data/repository/WalletRepository.kt`
- Modify: `app/src/main/java/com/example/homie/ui/main/wallet/WalletViewModel.kt`

- [ ] **Step 1: Expand `WalletRepository` with all needed methods**

  Replace the entire `WalletRepository.kt`:

  ```kotlin
  package com.example.homie.data.repository

  import com.example.homie.data.model.Expense
  import com.example.homie.data.model.User
  import com.google.firebase.auth.FirebaseAuth
  import com.google.firebase.firestore.FirebaseFirestore
  import kotlinx.coroutines.tasks.await

  class WalletRepository {

      private val firestore = FirebaseFirestore.getInstance()
      private val auth = FirebaseAuth.getInstance()

      suspend fun getApartmentId(): String? {
          val uid = auth.currentUser?.uid ?: return null
          return firestore.collection("users")
              .document(uid).get().await()
              .getString("apartmentId")
      }

      suspend fun getExpenses(aptId: String): List<Expense> {
          return firestore.collection("apartments")
              .document(aptId)
              .collection("expenses")
              .get().await()
              .documents.mapNotNull { it.toObject(Expense::class.java) }
      }

      suspend fun addExpense(aptId: String, expense: Expense) {
          firestore.collection("apartments")
              .document(aptId)
              .collection("expenses")
              .document(expense.id)
              .set(expense).await()
      }

      suspend fun deleteExpense(aptId: String, expenseId: String) {
          firestore.collection("apartments")
              .document(aptId)
              .collection("expenses")
              .document(expenseId)
              .delete().await()
      }

      suspend fun getApartmentMembers(aptId: String): List<User> {
          val aptDoc = firestore.collection("apartments")
              .document(aptId).get().await()
          val memberIds = aptDoc.get("members") as? List<String> ?: return emptyList()
          return memberIds.mapNotNull { uid ->
              firestore.collection("users")
                  .document(uid).get().await()
                  .toObject(User::class.java)
          }
      }
  }
  ```

- [ ] **Step 2: Replace the entire `WalletViewModel.kt`**

  ```kotlin
  package com.example.homie.ui.main.wallet

  import android.net.Uri
  import androidx.lifecycle.LiveData
  import androidx.lifecycle.MutableLiveData
  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.example.homie.data.model.Expense
  import com.example.homie.data.model.MemberBalance
  import com.example.homie.data.model.User
  import com.example.homie.data.repository.WalletRepository
  import com.google.firebase.auth.FirebaseAuth
  import com.google.firebase.firestore.FirebaseFirestore
  import com.google.firebase.storage.FirebaseStorage
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.tasks.await
  import java.util.UUID

  class WalletViewModel(
      private val repository: WalletRepository = WalletRepository()
  ) : ViewModel() {

      private val auth = FirebaseAuth.getInstance()
      private val firestore = FirebaseFirestore.getInstance()
      private val storage = FirebaseStorage.getInstance()

      private val _uiState = MutableLiveData<WalletUiState>()
      val uiState: LiveData<WalletUiState> = _uiState

      val expenseSaved = MutableLiveData<Boolean>()

      private var cachedApartmentId: String? = null

      private suspend fun getApartmentId(): String? {
          if (cachedApartmentId == null) {
              cachedApartmentId = repository.getApartmentId()
          }
          return cachedApartmentId
      }

      fun loadExpenses() {
          _uiState.value = WalletUiState.Loading
          viewModelScope.launch {
              try {
                  val aptId = getApartmentId() ?: throw Exception("No apartment found")
                  val members = repository.getApartmentMembers(aptId)
                  val expenses = repository.getExpenses(aptId)
                  val balanceRows = calculateBalance(members, expenses)
                  _uiState.value = WalletUiState.Success(expenses, balanceRows)
              } catch (e: Exception) {
                  _uiState.value = WalletUiState.Error(e.message ?: "Unknown error")
              }
          }
      }

      private fun calculateBalance(
          members: List<User>,
          expenses: List<Expense>
      ): List<MemberBalance> {
          if (members.isEmpty()) return emptyList()
          val balanceMap = mutableMapOf<String, Double>()
          members.forEach { balanceMap[it.userId] = 0.0 }

          expenses.forEach { expense ->
              val splitAmong = if (expense.participants.isEmpty()) {
                  members
              } else {
                  members.filter { it.userId in expense.participants }
              }
              if (splitAmong.isEmpty()) return@forEach

              val perPersonShare = expense.amount / splitAmong.size
              splitAmong.forEach { member ->
                  if (member.userId == expense.payerId) {
                      balanceMap[member.userId] =
                          balanceMap[member.userId]!! + (expense.amount - perPersonShare)
                  } else {
                      balanceMap[member.userId] =
                          balanceMap[member.userId]!! - perPersonShare
                  }
              }
          }

          return members.map { user ->
              MemberBalance(user.userId, user.name, balanceMap[user.userId] ?: 0.0)
          }
      }

      fun addExpense(amount: Double, category: String, description: String, imageUri: Uri?) {
          viewModelScope.launch {
              try {
                  val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                  val aptId = getApartmentId() ?: throw Exception("No apartment found")

                  var receiptUrl: String? = null
                  if (imageUri != null) {
                      val fileRef = storage.reference.child("receipts/${UUID.randomUUID()}.jpg")
                      fileRef.putFile(imageUri).await()
                      receiptUrl = fileRef.downloadUrl.await().toString()
                  }

                  val userDoc = firestore.collection("users")
                      .document(currentUser.uid).get().await()
                  val payerName = userDoc.getString("name") ?: currentUser.email ?: ""

                  val expenseId = UUID.randomUUID().toString()
                  val expense = Expense(
                      id = expenseId,
                      amount = amount,
                      category = category,
                      description = description,
                      payerId = currentUser.uid,
                      payerName = payerName,
                      receiptUrl = receiptUrl,
                      timestamp = System.currentTimeMillis()
                  )
                  repository.addExpense(aptId, expense)
                  expenseSaved.value = true
                  loadExpenses()
              } catch (e: Exception) {
                  expenseSaved.value = false
              }
          }
      }

      fun deleteExpense(expenseId: String) {
          viewModelScope.launch {
              try {
                  val aptId = getApartmentId() ?: return@launch
                  repository.deleteExpense(aptId, expenseId)
                  loadExpenses()
              } catch (e: Exception) {
                  _uiState.value = WalletUiState.Error(e.message ?: "Failed to delete")
              }
          }
      }

      fun settleUp(fromUserId: String, fromName: String, amount: Double) {
          viewModelScope.launch {
              try {
                  val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                  val aptId = getApartmentId() ?: throw Exception("No apartment found")

                  val userDoc = firestore.collection("users")
                      .document(currentUser.uid).get().await()
                  val toName = userDoc.getString("name") ?: currentUser.email ?: ""

                  val expenseId = UUID.randomUUID().toString()
                  val settlement = Expense(
                      id = expenseId,
                      amount = amount,
                      category = "Settlement",
                      description = "$fromName → $toName",
                      payerId = fromUserId,
                      payerName = fromName,
                      participants = listOf(fromUserId, currentUser.uid),
                      timestamp = System.currentTimeMillis()
                  )
                  repository.addExpense(aptId, settlement)
                  loadExpenses()
              } catch (e: Exception) {
                  _uiState.value = WalletUiState.Error(e.message ?: "Failed to settle")
              }
          }
      }
  }
  ```

- [ ] **Step 3: Build — expect compile errors in AddExpenseFragment and WalletFragment**

  ```bash
  ./gradlew assembleDebug
  ```

---

### Task 20: Update AddExpenseFragment to use `expenseSaved`

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/wallet/AddExpenseFragment.kt`

- [ ] **Step 1: Replace the body of `observeSaveState()`**

  Find the existing `observeSaveState()` function and replace the entire function with:

  ```kotlin
  private fun observeSaveState() {
      viewModel.expenseSaved.observe(viewLifecycleOwner) { saved ->
          progressDialog.dismiss()
          if (saved) {
              Toast.makeText(requireContext(), "Expense Added", Toast.LENGTH_SHORT).show()
              findNavController().popBackStack()
          } else {
              Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_LONG).show()
          }
      }
  }
  ```

  The call to `observeSaveState()` in `onViewCreated` stays unchanged.

- [ ] **Step 2: Update `validateAndSave()` to show progress dialog before calling `addExpense`**

  Find the existing `validateAndSave()` function and replace it with:

  ```kotlin
  private fun validateAndSave() {
      val amountText = binding.etAmount.text.toString()
      if (amountText.isEmpty()) {
          Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
          return
      }
      val description = binding.etDescription.text.toString()
      val category = binding.spinnerCategory.selectedItem.toString()
      progressDialog.show()
      viewModel.addExpense(
          amount = amountText.toDouble(),
          category = category,
          description = description,
          imageUri = selectedImageUri
      )
  }
  ```

- [ ] **Step 3: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL` (WalletFragment still broken until next task)

---

### Task 21: Create `item_balance_row.xml` and `BalanceSummaryAdapter`

**Files:**
- Create: `app/src/main/res/layout/item_balance_row.xml`
- Create: `app/src/main/java/com/example/homie/ui/main/wallet/BalanceSummaryAdapter.kt`

- [ ] **Step 1: Create `item_balance_row.xml`**

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:orientation="horizontal"
      android:padding="12dp"
      android:gravity="center_vertical"
      android:background="?attr/selectableItemBackground">

      <TextView
          android:id="@+id/tvMemberName"
          android:layout_width="0dp"
          android:layout_height="wrap_content"
          android:layout_weight="1"
          android:textSize="14sp"
          android:textColor="@color/text_primary" />

      <TextView
          android:id="@+id/tvBalance"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:textSize="14sp"
          android:textStyle="bold" />

  </LinearLayout>
  ```

- [ ] **Step 2: Create `BalanceSummaryAdapter.kt`**

  ```kotlin
  package com.example.homie.ui.main.wallet

  import android.view.LayoutInflater
  import android.view.ViewGroup
  import androidx.core.content.ContextCompat
  import androidx.recyclerview.widget.DiffUtil
  import androidx.recyclerview.widget.ListAdapter
  import androidx.recyclerview.widget.RecyclerView
  import com.example.homie.R
  import com.example.homie.data.model.MemberBalance
  import com.example.homie.databinding.ItemBalanceRowBinding

  class BalanceSummaryAdapter(
      private val onSettleClick: (MemberBalance) -> Unit
  ) : ListAdapter<MemberBalance, BalanceSummaryAdapter.BalanceViewHolder>(DiffCallback()) {

      inner class BalanceViewHolder(val binding: ItemBalanceRowBinding)
          : RecyclerView.ViewHolder(binding.root)

      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
          val binding = ItemBalanceRowBinding.inflate(
              LayoutInflater.from(parent.context), parent, false
          )
          return BalanceViewHolder(binding)
      }

      override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
          val item = getItem(position)
          holder.binding.tvMemberName.text = item.name
          holder.binding.tvBalance.text = "%.2f ₪".format(item.balance)

          val colorRes = when {
              item.balance > 0.01 -> R.color.success
              item.balance < -0.01 -> R.color.error
              else -> R.color.text_tertiary
          }
          holder.binding.tvBalance.setTextColor(
              ContextCompat.getColor(holder.binding.root.context, colorRes)
          )

          if (item.balance < -0.01) {
              holder.binding.root.setOnClickListener { onSettleClick(item) }
              holder.binding.root.isClickable = true
          } else {
              holder.binding.root.setOnClickListener(null)
              holder.binding.root.isClickable = false
          }
      }

      class DiffCallback : DiffUtil.ItemCallback<MemberBalance>() {
          override fun areItemsTheSame(old: MemberBalance, new: MemberBalance) =
              old.userId == new.userId
          override fun areContentsTheSame(old: MemberBalance, new: MemberBalance) =
              old == new
      }
  }
  ```

- [ ] **Step 3: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 22: Create Settle Up bottom sheet layout and fragment

**Files:**
- Create: `app/src/main/res/layout/fragment_settle_up_bottom_sheet.xml`
- Create: `app/src/main/java/com/example/homie/ui/main/wallet/SettleUpBottomSheet.kt`

- [ ] **Step 1: Create `fragment_settle_up_bottom_sheet.xml`**

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:app="http://schemas.android.com/apk/res-auto"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:orientation="vertical"
      android:padding="24dp">

      <TextView
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:text="Settle Up"
          android:textSize="20sp"
          android:textStyle="bold"
          android:textColor="@color/text_primary"
          android:layout_marginBottom="16dp" />

      <com.google.android.material.textfield.TextInputLayout
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:hint="From"
          style="@style/Widget.Material3.TextInputLayout.OutlinedBox">
          <com.google.android.material.textfield.TextInputEditText
              android:id="@+id/etFrom"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:enabled="false" />
      </com.google.android.material.textfield.TextInputLayout>

      <com.google.android.material.textfield.TextInputLayout
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:hint="To"
          android:layout_marginTop="12dp"
          style="@style/Widget.Material3.TextInputLayout.OutlinedBox">
          <com.google.android.material.textfield.TextInputEditText
              android:id="@+id/etTo"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:enabled="false" />
      </com.google.android.material.textfield.TextInputLayout>

      <com.google.android.material.textfield.TextInputLayout
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:hint="Amount (₪)"
          android:layout_marginTop="12dp"
          style="@style/Widget.Material3.TextInputLayout.OutlinedBox">
          <com.google.android.material.textfield.TextInputEditText
              android:id="@+id/etAmount"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:inputType="numberDecimal" />
      </com.google.android.material.textfield.TextInputLayout>

      <com.google.android.material.button.MaterialButton
          android:id="@+id/btnConfirm"
          android:layout_width="match_parent"
          android:layout_height="56dp"
          android:layout_marginTop="24dp"
          android:text="Confirm"
          android:textAllCaps="false"
          app:cornerRadius="16dp" />

  </LinearLayout>
  ```

- [ ] **Step 2: Create `SettleUpBottomSheet.kt`**

  ```kotlin
  package com.example.homie.ui.main.wallet

  import android.os.Bundle
  import android.view.LayoutInflater
  import android.view.View
  import android.view.ViewGroup
  import android.widget.Toast
  import com.example.homie.data.model.MemberBalance
  import com.example.homie.databinding.FragmentSettleUpBottomSheetBinding
  import com.google.android.material.bottomsheet.BottomSheetDialogFragment

  class SettleUpBottomSheet(
      private val fromMember: MemberBalance,
      private val currentUserName: String,
      private val onSettle: (fromUserId: String, fromName: String, amount: Double) -> Unit
  ) : BottomSheetDialogFragment() {

      private var _binding: FragmentSettleUpBottomSheetBinding? = null
      private val binding get() = _binding!!

      override fun onCreateView(
          inflater: LayoutInflater,
          container: ViewGroup?,
          savedInstanceState: Bundle?
      ): View {
          _binding = FragmentSettleUpBottomSheetBinding.inflate(inflater, container, false)
          return binding.root
      }

      override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
          binding.etFrom.setText(fromMember.name)
          binding.etTo.setText(currentUserName)
          // balance is negative (owes money), show absolute value
          binding.etAmount.setText("%.2f".format(-fromMember.balance))

          binding.btnConfirm.setOnClickListener {
              val amountText = binding.etAmount.text.toString().trim()
              if (amountText.isEmpty()) {
                  Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show()
                  return@setOnClickListener
              }
              val amount = amountText.toDoubleOrNull()
              if (amount == null || amount <= 0) {
                  Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                  return@setOnClickListener
              }
              onSettle(fromMember.userId, fromMember.name, amount)
              dismiss()
          }
      }

      override fun onDestroyView() {
          super.onDestroyView()
          _binding = null
      }
  }
  ```

- [ ] **Step 3: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

---

### Task 23: Update `fragment_wallet.xml` — replace `tvBalanceSummary` with RecyclerView

**Files:**
- Modify: `app/src/main/res/layout/fragment_wallet.xml`

- [ ] **Step 1: Inside `cardBalance`, replace the `AppCompatTextView` with a `RecyclerView`**

  Find the `<androidx.appcompat.widget.AppCompatTextView android:id="@+id/tvBalanceSummary" .../>` tag inside `cardBalance` and replace it with:

  ```xml
  <androidx.recyclerview.widget.RecyclerView
      android:id="@+id/rvBalanceSummary"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:padding="8dp"
      android:nestedScrollingEnabled="false" />
  ```

- [ ] **Step 2: Build — expect compile error in WalletFragment about `binding.tvBalanceSummary`**

  ```bash
  ./gradlew assembleDebug
  ```

---

### Task 24: Update WalletFragment to use BalanceSummaryAdapter and open settle-up sheet

**Files:**
- Modify: `app/src/main/java/com/example/homie/ui/main/wallet/WalletFragment.kt`

- [ ] **Step 1: Add new imports**

  ```kotlin
  import androidx.recyclerview.widget.LinearLayoutManager
  import com.google.firebase.auth.FirebaseAuth
  ```

- [ ] **Step 2: Add a `BalanceSummaryAdapter` field**

  Add alongside the existing `adapter` field:

  ```kotlin
  private lateinit var balanceAdapter: BalanceSummaryAdapter
  private var currentUserName = ""
  ```

- [ ] **Step 3: Set up `BalanceSummaryAdapter` in `onViewCreated`**

  After `binding.rvExpenses.adapter = adapter`, add:

  ```kotlin
  balanceAdapter = BalanceSummaryAdapter { member ->
      SettleUpBottomSheet(member, currentUserName) { fromUserId, fromName, amount ->
          viewModel.settleUp(fromUserId, fromName, amount)
      }.show(childFragmentManager, "settle_up")
  }
  binding.rvBalanceSummary.layoutManager = LinearLayoutManager(requireContext())
  binding.rvBalanceSummary.adapter = balanceAdapter
  ```

  `currentUserName` will be resolved from the balance rows in the `Success` observer (next step).

- [ ] **Step 4: Update `observeUiState()` — remove `tvBalanceSummary`, wire `balanceAdapter`**

  Replace the entire `observeUiState()` function:

  ```kotlin
  private fun observeUiState() {
      viewModel.uiState.observe(viewLifecycleOwner) { state ->
          when (state) {
              is WalletUiState.Loading -> {
                  if (!progressDialog.isShowing) progressDialog.show()
              }
              is WalletUiState.Success -> {
                  if (progressDialog.isShowing) progressDialog.dismiss()
                  adapter.submitList(state.expenses)
                  balanceAdapter.submitList(state.balanceRows)
                  // Resolve current user's name for the settle-up sheet
                  val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                  currentUserName = state.balanceRows
                      .firstOrNull { it.userId == currentUid }?.name ?: ""
              }
              is WalletUiState.Error -> {
                  if (progressDialog.isShowing) progressDialog.dismiss()
                  Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
              }
          }
      }
  }
  ```

- [ ] **Step 5: Build**

  ```bash
  ./gradlew assembleDebug
  ```
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Manual test**
  - Open Wallet → balance card shows per-member rows with colored amounts
  - A member with negative balance (owes money) shows a red amount and is tappable
  - Tap their row → settle-up sheet opens pre-filled with their name, your name, and owed amount
  - Edit the amount → tap Confirm → settlement appears in expense list as "Settlement" category
  - Balance recalculates and the settled member's balance moves toward zero
  - Members with positive / zero balance rows are not tappable
  - Add a new expense → navigates back from AddExpenseFragment → Wallet refreshes automatically

- [ ] **Step 7: Commit**

  ```bash
  git add app/src/main/java/com/example/homie/data/model/MemberBalance.kt \
          app/src/main/java/com/example/homie/data/model/Expense.kt \
          app/src/main/java/com/example/homie/ui/main/wallet/WalletUiState.kt \
          app/src/main/java/com/example/homie/data/repository/WalletRepository.kt \
          app/src/main/java/com/example/homie/ui/main/wallet/WalletViewModel.kt \
          app/src/main/java/com/example/homie/ui/main/wallet/AddExpenseFragment.kt \
          app/src/main/res/layout/item_balance_row.xml \
          app/src/main/java/com/example/homie/ui/main/wallet/BalanceSummaryAdapter.kt \
          app/src/main/res/layout/fragment_settle_up_bottom_sheet.xml \
          app/src/main/java/com/example/homie/ui/main/wallet/SettleUpBottomSheet.kt \
          app/src/main/res/layout/fragment_wallet.xml \
          app/src/main/java/com/example/homie/ui/main/wallet/WalletFragment.kt
  git commit -m "feat: settle up — per-member balance rows and settlement recording"
  ```
