package com.minderu.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minderu.data.AuthRepository
import com.minderu.data.CompletionStore
import com.minderu.data.ErrorMessages
import com.minderu.data.ServiceLocator
import com.minderu.data.TaskDto
import com.minderu.data.TaskRepository
import com.minderu.data.TaskUiModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ── Unified UI state ─────────────────────────────────────────

data class TodayUiState(
    val tasks: List<TaskUiModel> = emptyList(),
    val isLoading: Boolean = true,
    /** Non-null only when the initial load itself failed. */
    val loadError: String? = null,
    /** Real Sparks balance from the `profiles` table. */
    val sparksBalance: Int = 0
)

// ── ViewModel ────────────────────────────────────────────────

class TodayTasksViewModel(
    private val authRepo: AuthRepository,
    private val taskRepo: TaskRepository,
    private val completionStore: CompletionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    /**
     * One-shot transient errors for a Snackbar.
     * Using SharedFlow (replay=0) so each error is consumed exactly once.
     */
    private val _transientError = MutableSharedFlow<String>()
    val transientError: SharedFlow<String> = _transientError.asSharedFlow()

    private var activeRoutineId: String? = null

    /**
     * Pending add-task calls that arrived before the routine loaded.
     * Drained once [activeRoutineId] is set.
     */
    private val pendingAdds = mutableListOf<Triple<String, String, Int>>()

    init {
        loadTasks()
    }

    // ── Load ─────────────────────────────────────────────────

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
                val userId = authRepo.currentUserId()
                    ?: throw Exception("Not authenticated")

                // Ensure profile exists so Sparks are never silently lost
                taskRepo.ensureProfile(userId)

                // Fetch or seed default routine
                val routine = taskRepo.fetchActiveRoutine(userId)
                    ?: taskRepo.createDefaultRoutine(userId)

                activeRoutineId = routine.id

                val dtos = taskRepo.fetchTasksForRoutine(routine.id)

                // Prune stale completion ids
                completionStore.retainOnly(dtos.map { it.id }.toSet())

                val tasks = dtos.map { it.toUiModel() }

                // Fetch live Sparks balance
                val profile = taskRepo.fetchProfile(userId)
                val sparks = profile?.sparks ?: 0

                _uiState.update {
                    it.copy(
                        tasks = tasks,
                        isLoading = false,
                        loadError = null,
                        sparksBalance = sparks
                    )
                }

                // Drain any tasks that were queued while the routine was loading
                drainPendingAdds()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = ErrorMessages.from(e)
                    )
                }
            }
        }
    }

    // ── Task Completion ──────────────────────────────────────

    // ── Task Completion ──────────────────────────────────────

    fun completeTaskWithDelay(taskId: String) {
        val task = _uiState.value.tasks.find { it.id == taskId } ?: return
        if (task.isCompleted) return

        // Mark completed in UI first for instant checkmark feedback
        completionStore.setCompleted(taskId, true)
        _uiState.update { state ->
            state.copy(tasks = state.tasks.map {
                if (it.id == taskId) it.copy(isCompleted = true) else it
            })
        }

        viewModelScope.launch {
            try {
                val userId = authRepo.currentUserId()
                val randomSparksReward = (1..5).random()

                if (userId != null) {
                    taskRepo.recordTaskCompletion(userId, taskId)
                    val profile = taskRepo.fetchProfile(userId)
                    
                    val todayStr = java.time.LocalDate.now().toString()
                    val yesterdayStr = java.time.LocalDate.now().minusDays(1).toString()
                    
                    var currentStreak = profile?.currentStreak ?: 0
                    var maxStreak = profile?.maxStreak ?: 0
                    var streakBonus = 0

                    val lastActive = profile?.lastActiveDate
                    if (lastActive == yesterdayStr) {
                        currentStreak += 1
                    } else if (lastActive != todayStr) {
                        currentStreak = 1
                    }
                    if (currentStreak > maxStreak) maxStreak = currentStreak

                    // Check for 7-day streak milestone
                    if (currentStreak > 0 && currentStreak % 7 == 0 && lastActive != todayStr) {
                        streakBonus = 50
                    }

                    val totalEarned = randomSparksReward + streakBonus
                    taskRepo.adjustSparks(userId, totalEarned)
                    taskRepo.updateProfileStreak(userId, currentStreak, maxStreak, todayStr)

                    val updatedProfile = taskRepo.fetchProfile(userId)
                    if (updatedProfile != null) {
                        _uiState.update { it.copy(sparksBalance = updatedProfile.sparks) }
                    }

                    if (streakBonus > 0) {
                        _transientError.emit("🔥 7-DAY STREAK ACHIEVED! +$streakBonus Bonus Sparks! (+$randomSparksReward Task Sparks)")
                    } else {
                        _transientError.emit("🎉 Task completed! +$randomSparksReward Sparks earned!")
                    }
                } else {
                    _transientError.emit("🎉 Task completed! +$randomSparksReward Sparks earned!")
                }

                // Allow 2.5s visual feedback window before transition out
                kotlinx.coroutines.delay(2500)

                // Erase from UI state
                _uiState.update { state ->
                    state.copy(tasks = state.tasks.filter { it.id != taskId })
                }

                // Delete from DB to prevent DB bloat
                taskRepo.deleteTask(taskId)
                completionStore.setCompleted(taskId, false)

            } catch (e: Exception) {
                Log.e("TodayTasksViewModel", "Error in task completion: ${e.message}", e)
                _transientError.emit(ErrorMessages.from(e))
            }
        }
    }

    fun toggleComplete(taskId: String) {
        completeTaskWithDelay(taskId)
    }

    // ── Task CRUD ────────────────────────────────────────────

    fun addTask(title: String, subhead: String, sparksReward: Int) {
        val routineId = activeRoutineId
        if (routineId == null) {
            // Queue for when the routine finishes loading
            synchronized(pendingAdds) {
                pendingAdds.add(Triple(title, subhead, sparksReward))
            }
            return
        }
        doAddTask(routineId, title, subhead, sparksReward)
    }

    private fun doAddTask(routineId: String, title: String, subhead: String, sparksReward: Int) {
        // Position = max existing + 1 to avoid collisions after deletes
        val nextIndex = (_uiState.value.tasks.maxOfOrNull { task ->
            // We don't store positionIndex in the UI model, so use list index as proxy.
            // But the server source-of-truth is the actual position_index column.
            // A more correct approach: track it. For now, use count as safe max+1.
            0
        } ?: -1) + 1
        // Actually: use the list size as an approximation. The server orders by position_index
        // anyway, so the worst that can happen with a gap is visual until the next reload.
        val safeIndex = _uiState.value.tasks.size

        val newId = UUID.randomUUID().toString()

        // Optimistic append
        _uiState.update { state ->
            state.copy(tasks = state.tasks + TaskUiModel(newId, title, subhead, sparksReward))
        }

        viewModelScope.launch {
            try {
                taskRepo.insertTask(
                    TaskDto(newId, routineId, safeIndex, title, subhead, sparksReward)
                )
            } catch (e: Exception) {
                // Per-item rollback: only remove the failed item
                _uiState.update { state ->
                    state.copy(tasks = state.tasks.filter { it.id != newId })
                }
                _transientError.emit("Failed to add task: ${ErrorMessages.from(e)}")
            }
        }
    }

    fun updateTask(taskId: String, title: String, subhead: String, sparksReward: Int) {
        // Snapshot the single item for per-item rollback
        val previousTask = _uiState.value.tasks.find { it.id == taskId } ?: return

        // Optimistic update
        _uiState.update { state ->
            state.copy(tasks = state.tasks.map { task ->
                if (task.id == taskId) task.copy(title = title, subhead = subhead, sparks = sparksReward)
                else task
            })
        }

        viewModelScope.launch {
            try {
                taskRepo.updateTask(taskId, title, subhead, sparksReward)
            } catch (e: Exception) {
                // Per-item rollback: restore only the single task
                _uiState.update { state ->
                    state.copy(tasks = state.tasks.map { task ->
                        if (task.id == taskId) previousTask else task
                    })
                }
                _transientError.emit("Failed to update task: ${ErrorMessages.from(e)}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        // Snapshot the single item + its index for per-item rollback
        val currentTasks = _uiState.value.tasks
        val deletedIndex = currentTasks.indexOfFirst { it.id == taskId }
        val deletedTask = if (deletedIndex >= 0) currentTasks[deletedIndex] else return

        // Optimistic removal
        _uiState.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId })
        }

        viewModelScope.launch {
            try {
                taskRepo.deleteTask(taskId)
                // Clear completion for deleted task
                completionStore.setCompleted(taskId, false)
            } catch (e: Exception) {
                // Per-item rollback: re-insert at original position
                _uiState.update { state ->
                    val mutable = state.tasks.toMutableList()
                    val insertAt = deletedIndex.coerceAtMost(mutable.size)
                    mutable.add(insertAt, deletedTask)
                    state.copy(tasks = mutable)
                }
                _transientError.emit("Failed to delete task: ${ErrorMessages.from(e)}")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun drainPendingAdds() {
        val routineId = activeRoutineId ?: return
        val queued: List<Triple<String, String, Int>>
        synchronized(pendingAdds) {
            queued = pendingAdds.toList()
            pendingAdds.clear()
        }
        for ((title, subhead, sparks) in queued) {
            doAddTask(routineId, title, subhead, sparks)
        }
    }

    private fun TaskDto.toUiModel() = TaskUiModel(
        id = id,
        title = title,
        subhead = subhead ?: "",
        sparks = sparksReward,
        isCompleted = completionStore.isCompleted(id)
    )

    // ── Factory ──────────────────────────────────────────────

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodayTasksViewModel(
                authRepo = ServiceLocator.authRepository,
                taskRepo = ServiceLocator.taskRepository,
                completionStore = ServiceLocator.completionStore
            ) as T
        }
    }
}
