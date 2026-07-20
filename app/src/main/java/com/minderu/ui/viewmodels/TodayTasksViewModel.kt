package com.minderu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minderu.data.AuthRepository
import com.minderu.data.TaskDto
import com.minderu.data.TaskRepository
import com.minderu.data.TaskUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TodayTasksViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val taskRepo = TaskRepository()

    private val _tasks = MutableStateFlow<List<TaskUiModel>>(emptyList())
    val tasks: StateFlow<List<TaskUiModel>> = _tasks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeRoutineId: String? = null

    init {
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val userId = authRepo.currentUserId()
                    ?: throw Exception("Not authenticated")

                // Fetch or seed default routine
                val routine = taskRepo.fetchActiveRoutine(userId)
                    ?: taskRepo.createDefaultRoutine(userId)

                activeRoutineId = routine.id

                val dtos = taskRepo.fetchTasksForRoutine(routine.id)
                _tasks.value = dtos.map { it.toUiModel() }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load tasks"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Task Completion (Optimistic) ──────────────────────────

    fun toggleComplete(taskId: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
        }

        // Award sparks async if completing (not un-completing)
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.isCompleted) {
            viewModelScope.launch {
                try {
                    val userId = authRepo.currentUserId() ?: return@launch
                    taskRepo.awardSparks(userId, task.sparks)
                } catch (_: Exception) { /* best-effort background sync */ }
            }
        }
    }

    // ── Task CRUD ─────────────────────────────────────────────

    fun addTask(title: String, subhead: String, sparksReward: Int) {
        val routineId = activeRoutineId ?: return
        val nextIndex = (_tasks.value.size)
        val newId = UUID.randomUUID().toString()

        // Optimistic append
        _tasks.value = _tasks.value + TaskUiModel(newId, title, subhead, sparksReward)

        viewModelScope.launch {
            try {
                taskRepo.insertTask(
                    TaskDto(newId, routineId, nextIndex, title, subhead, sparksReward)
                )
            } catch (e: Exception) {
                // Roll back on failure
                _tasks.value = _tasks.value.filter { it.id != newId }
                _errorMessage.value = "Failed to add task: ${e.message}"
            }
        }
    }

    fun updateTask(taskId: String, title: String, subhead: String, sparksReward: Int) {
        // Optimistic update
        val previous = _tasks.value
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(title = title, subhead = subhead, sparks = sparksReward)
            else task
        }

        viewModelScope.launch {
            try {
                taskRepo.updateTask(taskId, title, subhead, sparksReward)
            } catch (e: Exception) {
                _tasks.value = previous
                _errorMessage.value = "Failed to update task: ${e.message}"
            }
        }
    }

    fun deleteTask(taskId: String) {
        // Optimistic removal
        val previous = _tasks.value
        _tasks.value = _tasks.value.filter { it.id != taskId }

        viewModelScope.launch {
            try {
                taskRepo.deleteTask(taskId)
            } catch (e: Exception) {
                _tasks.value = previous
                _errorMessage.value = "Failed to delete task: ${e.message}"
            }
        }
    }

    // ── Mapping ───────────────────────────────────────────────

    private fun TaskDto.toUiModel() = TaskUiModel(
        id = id,
        title = title,
        subhead = subhead,
        sparks = sparksReward,
        isCompleted = false
    )
}
