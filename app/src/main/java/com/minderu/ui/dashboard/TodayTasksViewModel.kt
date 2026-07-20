package com.minderu.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minderu.data.AuthRepository
import com.minderu.data.ProfileRepository
import com.minderu.data.RoutineRepository
import com.minderu.data.TaskRepository
import com.minderu.data.TaskUiModel
import com.minderu.data.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayTasksUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val tasks: List<TaskUiModel> = emptyList(),
    val routineId: String? = null,
    val sparksBalance: Int = 0,
    val isAnonymous: Boolean = true,
    val snackbarMessage: String? = null
)

class TodayTasksViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val routineRepository: RoutineRepository = RoutineRepository(),
    private val taskRepository: TaskRepository = TaskRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayTasksUiState())
    val uiState: StateFlow<TodayTasksUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun retry() = loadDashboard()

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }
            authRepository.ensureSession()
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Could not sign in"
                        )
                    }
                    return@launch
                }

            val userId = authRepository.currentUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Authentication failed")
                }
                return@launch
            }

            val routineResult = routineRepository.getOrCreateActiveRoutine(userId)
            val routine = routineResult.getOrElse { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Could not load routine"
                    )
                }
                return@launch
            }

            val tasksResult = taskRepository.listByRoutine(routine.id)
            val tasks = tasksResult.getOrElse { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Could not load tasks"
                    )
                }
                return@launch
            }

            val sparks = profileRepository.getProfile(userId).getOrNull()?.sparks ?: 0

            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = null,
                    routineId = routine.id,
                    tasks = tasks.map { dto -> dto.toUiModel() },
                    sparksBalance = sparks,
                    isAnonymous = authRepository.isAnonymousUser()
                )
            }
        }
    }

    /**
     * Completion is session-local; sparks persist via [ProfileRepository.addSparks].
     */
    fun completeTask(taskId: String) {
        val current = _uiState.value
        val index = current.tasks.indexOfFirst { it.id == taskId }
        if (index == -1) return

        val task = current.tasks[index]
        val markingComplete = !task.isCompleted

        _uiState.update { state ->
            val updated = state.tasks.toMutableList()
            updated[index] = task.copy(isCompleted = markingComplete)
            state.copy(tasks = updated)
        }

        if (!markingComplete) return

        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            profileRepository.addSparks(userId, task.sparks)
                .onSuccess { newBalance ->
                    _uiState.update { it.copy(sparksBalance = newBalance) }
                }
                .onFailure {
                    _uiState.update { state ->
                        val reverted = state.tasks.toMutableList()
                        val idx = reverted.indexOfFirst { it.id == taskId }
                        if (idx != -1) {
                            reverted[idx] = reverted[idx].copy(isCompleted = false)
                        }
                        state.copy(
                            tasks = reverted,
                            snackbarMessage = "Could not save sparks. Try again."
                        )
                    }
                }
        }
    }

    fun createTask(title: String, subhead: String, sparksReward: Int) {
        val routineId = _uiState.value.routineId ?: return
        val nextIndex = (_uiState.value.tasks.maxOfOrNull { it.positionIndex } ?: -1) + 1

        viewModelScope.launch {
            taskRepository.create(routineId, nextIndex, title, subhead, sparksReward)
                .onSuccess { dto ->
                    _uiState.update { state ->
                        state.copy(tasks = state.tasks + dto.toUiModel())
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(snackbarMessage = e.message ?: "Could not create task")
                    }
                }
        }
    }

    fun updateTask(taskId: String, title: String, subhead: String, sparksReward: Int) {
        viewModelScope.launch {
            taskRepository.update(taskId, title, subhead, sparksReward)
                .onSuccess { dto ->
                    _uiState.update { state ->
                        state.copy(
                            tasks = state.tasks.map { task ->
                                if (task.id == taskId) {
                                    dto.toUiModel(isCompleted = task.isCompleted)
                                } else {
                                    task
                                }
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(snackbarMessage = e.message ?: "Could not update task")
                    }
                }
        }
    }

    fun deleteTask(taskId: String) {
        val snapshot = _uiState.value.tasks
        val removed = snapshot.find { it.id == taskId } ?: return

        _uiState.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId })
        }

        viewModelScope.launch {
            taskRepository.delete(taskId)
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            tasks = snapshot,
                            snackbarMessage = "Could not delete task"
                        )
                    }
                }
        }
    }

    fun linkOrSignUpAccount(
        email: String,
        password: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (authRepository.isAnonymousUser()) {
                authRepository.linkAnonymousToEmail(email, password)
            } else {
                authRepository.signUpWithEmail(email, password)
            }
            result.onSuccess {
                _uiState.update {
                    it.copy(isAnonymous = authRepository.isAnonymousUser())
                }
            }
            onComplete(result)
        }
    }
}
