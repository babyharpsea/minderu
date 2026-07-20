package com.minderu.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo

class AuthRepository(
    private val provider: SupabaseProvider = SupabaseProvider
) {
    suspend fun ensureSession(): Result<Unit> = provider.ensureAuthenticated()

    fun currentUser(): UserInfo? = provider.client.auth.currentUserOrNull()

    fun currentUserId(): String? = currentUser()?.id

    fun isAnonymousUser(): Boolean {
        val user = currentUser() ?: return true
        return user.email.isNullOrBlank()
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            provider.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkAnonymousToEmail(email: String, password: String): Result<Unit> {
        return try {
            provider.client.auth.updateUser {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
