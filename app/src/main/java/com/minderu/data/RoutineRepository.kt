package com.minderu.data

import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID

class RoutineRepository(
    private val provider: SupabaseProvider = SupabaseProvider
) {
    suspend fun getOrCreateActiveRoutine(userId: String): Result<RoutineDto> {
        return try {
            val routines = provider.client.postgrest["routines"]
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_active", true)
                    }
                }
                .decodeList<RoutineDto>()

            val routine = routines.firstOrNull() ?: createDefaultRoutine(userId).getOrThrow()
            Result.success(routine)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createDefaultRoutine(userId: String): Result<RoutineDto> {
        return try {
            val newRoutine = RoutineDto(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "Daily Focus",
                isActive = true
            )
            provider.client.postgrest["routines"].insert(newRoutine)

            val defaultTasks = defaultTaskSeeds.mapIndexed { index, seed ->
                TaskDto(
                    routineId = newRoutine.id,
                    positionIndex = index,
                    title = seed.title,
                    subhead = seed.subhead,
                    sparksReward = seed.sparksReward
                )
            }
            provider.client.postgrest["tasks"].insert(defaultTasks)

            Result.success(newRoutine)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
