# Full Architecture Refactor & Duolingo-Style Auth for Minderu

Restructure the app from a flat "everything-in-composable" pattern to clean MVVM, replace anonymous auth with email/password onboarding, and support full Task CRUD.

## Proposed Changes

### Data Layer — DTOs, Repository, SupabaseClient

#### [MODIFY] [SupabaseClient.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/data/SupabaseClient.kt)
- Remove `SupabaseProvider.ensureAuthenticated()` (anonymous login).
- The SDK's built-in `SettingsSessionManager` already persists sessions to `SharedPreferences` automatically — no custom persistence code needed.
- Add `awaitInitialization()` usage so the session is loaded from disk before checking auth state.
- Rename `SupabaseProvider` to `SupabaseModule` and expose the singleton client only.

#### [MODIFY] [Models.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/data/Models.kt)
- Keep existing `RoutineDto`, `TaskDto` (already well-aligned with the DB schema).
- Add a `ProfileDto` for the `profiles` table.
- Keep `Task` as `TaskUiModel` (rename for clarity) with `isCompleted` presentation state.
- Add navigation routes for `Auth` screen: `sealed class Screen` gains `Auth` object.
- Keep existing `BinderCard`, `Planting`, mock data, and navigation enums untouched.

#### [NEW] [AuthRepository.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/data/AuthRepository.kt)
- `signUp(email, password)` → calls `auth.signUpWith(Email) { … }`.
- `signIn(email, password)` → calls `auth.signInWith(Email) { … }`.
- `signOut()` → calls `auth.signOut()`.
- `sessionStatusFlow()` → exposes `auth.sessionStatus` as a `Flow<SessionStatus>`.
- `currentUserId()` → returns the current user's UUID or null.

#### [NEW] [TaskRepository.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/data/TaskRepository.kt)
- `fetchActiveRoutine(userId)` → SELECT from `routines` where `user_id` = userId & `is_active` = true.
- `createDefaultRoutine(userId)` → INSERT "Daily Focus" routine + seed 3 default tasks.
- `fetchTasksForRoutine(routineId)` → SELECT from `tasks` ordered by `position_index`.
- `insertTask(dto)` → INSERT into `tasks`.
- `updateTask(dto)` → UPDATE `tasks` SET title, subhead, sparks_reward WHERE id.
- `deleteTask(taskId)` → DELETE FROM `tasks` WHERE id.
- `fetchProfile(userId)` → SELECT from `profiles`.
- `updateSparks(userId, delta)` → UPDATE `profiles` SET sparks = sparks + delta.

---

### ViewModel Layer

#### [NEW] [AuthViewModel.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/ui/viewmodels/AuthViewModel.kt)
- Exposes `sessionStatus: StateFlow<SessionStatus>` (collected from `AuthRepository`).
- `signUp(email, password)` / `signIn(email, password)` / `signOut()` → delegates to repository.
- Exposes `uiState: StateFlow<AuthUiState>` with `Idle`, `Loading`, `Error(message)` states for the auth dialogs.

#### [NEW] [TodayTasksViewModel.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/ui/viewmodels/TodayTasksViewModel.kt)
- Holds `tasks: StateFlow<List<TaskUiModel>>`, `isLoading`, `errorMessage`, `activeRoutineId`.
- `loadTasks()` → fetch active routine (or seed default), then fetch tasks.
- `toggleComplete(taskId)` → **optimistic UI flip** → async update sparks in `profiles`.
- `addTask(title, subhead, sparks)` → calculate `position_index` = max + 1, insert via repo, append to state.
- `updateTask(id, title, subhead, sparks)` → update via repo, patch state list.
- `deleteTask(taskId)` → optimistic removal from state → async DELETE via repo.

---

### UI Layer

#### [NEW] [AuthScreen.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/ui/screens/AuthScreen.kt)
Duolingo-style welcome screen:
- Full-screen layout with Minderu logo/title, tagline, and illustration.
- **"Get Started"** button → opens a Sign Up `ModalBottomSheet` (email + password fields).
- **"I already have an account"** text button → opens a Log In `ModalBottomSheet`.
- Both sheets: email field, password field, submit button, loading indicator, error text.
- On success → navigation handled by `MainActivity` observing session status.

#### [MODIFY] [AddTaskBottomSheet.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/ui/components/AddTaskBottomSheet.kt)
- Accept optional `existingTask: TaskUiModel?` parameter.
- If `existingTask != null` → **Edit mode**: pre-populate title, subhead, sparks; button reads "Save Changes".
- If `null` → **Create mode**: blank fields; button reads "Create Task".
- Add a `subhead` `OutlinedTextField` (currently missing — DB schema requires it).
- `onSubmit` callback changes to `(title: String, subhead: String, sparks: Int) -> Unit`.

#### [MODIFY] [TodayTasksScreen.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/ui/screens/TodayTasksScreen.kt)
- Remove **all** inline Supabase calls and coroutine scope logic.
- Accept `viewModel: TodayTasksViewModel` (created via `viewModel()` factory).
- Collect `viewModel.tasks`, `viewModel.isLoading`, `viewModel.errorMessage` as Compose state.
- Wire `onTaskCompleted` → `viewModel.toggleComplete(task.id)`.
- Wire `onTaskDeleted` → `viewModel.deleteTask(task.id)`.
- Add retry button on error state.
- Add sign-out option (overflow menu or settings icon in header) → calls `authViewModel.signOut()`.

#### [MODIFY] [TaskComponents.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/ui/components/TaskComponents.kt)
- Add `onTaskEdit: (Task) -> Unit` callback to `TaskCard` for triggering edit mode.
- Add a subtle edit icon button next to the delete button.

#### [MODIFY] [MainActivity.kt](file:///c:/Users/sergio/StudioProjects/minderu/app/src/main/java/com/minderu/MainActivity.kt)
- Create `AuthViewModel` at the activity/app level.
- Observe `sessionStatus`:
  - `LoadingFromStorage` → show full-screen M3 spinner.
  - `NotAuthenticated` → navigate to `AuthScreen`.
  - `Authenticated` → navigate to main `NavHost` (dashboard/binder/impact).
- Move `AddTaskBottomSheet` invocation into `TodayTasksScreen` (co-located with the ViewModel).
- Update `NavHost` to include `Screen.Auth.route`.

---

### Build Configuration

#### [MODIFY] [build.gradle.kts (app)](file:///c:/Users/sergio/StudioProjects/minderu/app/build.gradle.kts)
- Add `implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")` for `viewModel()` factory.
- Add `implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")` for `collectAsStateWithLifecycle()`.

---

## New Package Structure

```
com.minderu/
├── data/
│   ├── Models.kt            (DTOs + UI models + navigation)
│   ├── SupabaseClient.kt    (singleton client config)
│   ├── AuthRepository.kt    [NEW]
│   └── TaskRepository.kt    [NEW]
├── ui/
│   ├── viewmodels/
│   │   ├── AuthViewModel.kt      [NEW]
│   │   └── TodayTasksViewModel.kt [NEW]
│   ├── screens/
│   │   ├── AuthScreen.kt         [NEW]
│   │   ├── TodayTasksScreen.kt   (refactored)
│   │   ├── CardBinderScreen.kt   (untouched)
│   │   └── ImpactTrackerScreen.kt(untouched)
│   ├── components/
│   │   ├── AddTaskBottomSheet.kt (create+edit modes)
│   │   ├── TaskComponents.kt    (+ edit button)
│   │   └── NavigationComponents.kt (untouched)
│   └── theme/
│       └── Theme.kt             (untouched)
└── MainActivity.kt              (session-aware root)
```

## User Review Required

> [!IMPORTANT]
> **Supabase Key Exposure:** The current `SupabaseConfig.KEY` is hardcoded as a constant. This is the *anon/publishable* key which is safe for client-side use, but I want to confirm you're OK keeping it as-is. If you'd like, I can move it to `BuildConfig` via `buildConfigField` instead.

> [!IMPORTANT]
> **Anonymous Auth Migration:** Existing anonymous users will **not** be migrated — they have no email/password. The new flow requires real credentials. Any data tied to anonymous accounts will effectively be orphaned. Is that acceptable?

## Open Questions

> [!NOTE]
> **Sign-out placement:** Should the sign-out button be an icon in the `TodayTasksScreen` header, an item in a dropdown menu, or somewhere else (like a dedicated Settings/Profile screen)?

> [!NOTE]
> **Edge-to-edge status bar:** The current XML theme uses opaque white status/nav bars. Do you want me to switch to transparent edge-to-edge bars with `enableEdgeToEdge()` in `MainActivity`, or keep the current look?

## Verification Plan

### Automated Tests
- `./gradlew assembleDebug` — ensures the full project compiles without errors.

### Manual Verification
- Launch on device/emulator → verify auth screen appears → sign up → lands on dashboard → tasks load → can create/edit/delete tasks → sign out → returns to auth → sign back in → session persists across kill/restart.
