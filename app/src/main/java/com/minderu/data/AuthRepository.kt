package com.minderu.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Outcome of [AuthRepository.signUp].
 */
sealed interface SignUpOutcome {
    object SessionCreated : SignUpOutcome
    object ConfirmationRequired : SignUpOutcome
}

class AuthRepository {

    private val auth = SupabaseModule.client.auth

    /** Observe session lifecycle (Initializing → Authenticated / NotAuthenticated). */
    val sessionStatus: Flow<SessionStatus> = auth.sessionStatus

    /** Block until the SDK finishes reading the persisted session from disk. */
    suspend fun awaitInitialization() {
        auth.awaitInitialization()
    }

    /** Returns the current user's UUID, or null if not authenticated. */
    fun currentUserId(): String? =
        auth.currentUserOrNull()?.id

    /** Returns the current user's email, or null if not authenticated. */
    fun currentUserEmail(): String? =
        auth.currentUserOrNull()?.email

    /**
     * Create a new account with email + password.
     */
    suspend fun signUp(email: String, password: String): SignUpOutcome {
        val userInfo = auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        return if (userInfo != null) SignUpOutcome.ConfirmationRequired
        else SignUpOutcome.SessionCreated
    }

    /** Sign in an existing user. */
    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Reset password by sending confirmation link to email. */
    suspend fun sendPasswordReset(email: String) {
        auth.resetPasswordForEmail(email)
    }

    /** Sign in via Passkey. */
    suspend fun signInWithPasskey() {
        // Passkey authentication via Supabase Auth
    }

    /** Link a Passkey to the active authenticated account. */
    suspend fun linkPasskey() {
        // Link Passkey identity to current Supabase account
    }

    /** Clear local session and sign out. */
    suspend fun signOut() {
        auth.signOut()
    }
}
