package com.minderu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minderu.data.AuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI state for auth dialogs (sign-up / sign-in sheets). */
sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    /** Reactive session lifecycle for root navigation decisions. */
    val sessionStatus: StateFlow<SessionStatus> = repo.sessionStatus
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionStatus.Initializing)

    /** Auth dialog state (loading spinner, error text). */
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                repo.signUp(email, password)
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Sign-up failed"
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                repo.signIn(email, password)
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Sign-in failed"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                repo.signOut()
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }
}
