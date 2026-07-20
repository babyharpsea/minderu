package com.minderu.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ProfileRepository(
    private val provider: SupabaseProvider = SupabaseProvider
) {
    suspend fun getProfile(userId: String): Result<ProfileDto> {
        return try {
            val profile = provider.client.postgrest["profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<ProfileDto>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSparks(userId: String, delta: Int): Result<Int> {
        if (delta <= 0) {
            return getProfile(userId).map { it.sparks }
        }
        return try {
            val current = getProfile(userId).getOrThrow()
            val newSparks = current.sparks + delta
            provider.client.postgrest["profiles"]
                .update(ProfileSparksPatch(sparks = newSparks)) {
                    filter { eq("id", userId) }
                }
            Result.success(newSparks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
private data class ProfileSparksPatch(
    val sparks: Int
)
