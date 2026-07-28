package com.minderu.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

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

    /** Create a new account with email + password. */
    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Sign in an existing user. */
    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Clear local session and sign out. */
    suspend fun signOut() {
        auth.signOut()
    }
}
