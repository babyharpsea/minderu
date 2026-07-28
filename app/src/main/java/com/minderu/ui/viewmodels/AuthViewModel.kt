package com.minderu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minderu.data.AuthRepository
import com.minderu.data.AuthSanitizer
import com.minderu.data.ErrorMessages
import com.minderu.data.SignUpOutcome
import com.minderu.data.ServiceLocator
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object ConfirmationSent : AuthUiState
    object PasswordResetSent : AuthUiState
    data class Error(val message: String) : AuthUiState
}

enum class AuthSheet { SignUp, SignIn }

data class FieldErrors(
    val email: String? = null,
    val password: String? = null
) {
    val hasErrors: Boolean get() = email != null || password != null
}

class AuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus> = repo.sessionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionStatus.Initializing)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _activeSheet = MutableStateFlow<AuthSheet?>(null)
    val activeSheet: StateFlow<AuthSheet?> = _activeSheet.asStateFlow()

    private val _fieldErrors = MutableStateFlow(FieldErrors())
    val fieldErrors: StateFlow<FieldErrors> = _fieldErrors.asStateFlow()

    fun openSheet(sheet: AuthSheet) {
        _activeSheet.value = sheet
        _uiState.value = AuthUiState.Idle
        _fieldErrors.value = FieldErrors()
    }

    fun closeSheet() {
        _activeSheet.value = null
        _uiState.value = AuthUiState.Idle
        _fieldErrors.value = FieldErrors()
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
        _fieldErrors.value = FieldErrors()
    }

    fun currentUserEmail(): String? = repo.currentUserEmail()

    // ── Actions ──────────────────────────────────────────────

    fun signUp(email: String, password: String) {
        val (cleanEmail, cleanPassword) = sanitizeAndValidate(email, password) ?: return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                when (repo.signUp(cleanEmail, cleanPassword)) {
                    SignUpOutcome.SessionCreated -> {
                        _uiState.value = AuthUiState.Idle
                        _activeSheet.value = null
                    }
                    SignUpOutcome.ConfirmationRequired -> {
                        _uiState.value = AuthUiState.ConfirmationSent
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(ErrorMessages.from(e))
            }
        }
    }

    fun signIn(email: String, password: String) {
        val (cleanEmail, cleanPassword) = sanitizeAndValidate(email, password) ?: return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                repo.signIn(cleanEmail, cleanPassword)
                _uiState.value = AuthUiState.Idle
                _activeSheet.value = null
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(ErrorMessages.from(e))
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val (cleanEmail, emailResult) = AuthSanitizer.sanitizeEmail(email)
        if (emailResult is AuthSanitizer.EmailValidationResult.Invalid) {
            _fieldErrors.value = FieldErrors(email = emailResult.reason)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                repo.sendPasswordReset(cleanEmail)
                _uiState.value = AuthUiState.PasswordResetSent
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(ErrorMessages.from(e))
            }
        }
    }

    fun signInWithPasskey() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                repo.signInWithPasskey()
                _uiState.value = AuthUiState.Idle
                _activeSheet.value = null
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(ErrorMessages.from(e))
            }
        }
    }

    fun linkPasskey() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                repo.linkPasskey()
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(ErrorMessages.from(e))
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

    // ── Validation & Sanitization Helpers ────────────────────

    private fun sanitizeAndValidate(email: String, password: String): Pair<String, String>? {
        val (cleanEmail, emailResult) = AuthSanitizer.sanitizeEmail(email)
        val (cleanPassword, passwordResult) = AuthSanitizer.sanitizePassword(password)

        val emailErr = (emailResult as? AuthSanitizer.EmailValidationResult.Invalid)?.reason
        val passwordErr = (passwordResult as? AuthSanitizer.PasswordValidationResult.Invalid)?.reason

        _fieldErrors.value = FieldErrors(email = emailErr, password = passwordErr)

        return if (emailErr == null && passwordErr == null) {
            Pair(cleanEmail, cleanPassword)
        } else {
            null
        }
    }

    // ── Factory ──────────────────────────────────────────────

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(ServiceLocator.authRepository) as T
        }
    }
}
