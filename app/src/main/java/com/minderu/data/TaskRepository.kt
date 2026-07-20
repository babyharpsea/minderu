package com.minderu.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class TaskRepository(
    private val provider: SupabaseProvider = SupabaseProvider
) {
    suspend fun listByRoutine(routineId: String): Result<List<TaskDto>> {
        return try {
            val tasks = provider.client.postgrest["tasks"]
                .select {
                    filter { eq("routine_id", routineId) }
                }
                .decodeList<TaskDto>()
                .sortedBy { it.positionIndex }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun create(
        routineId: String,
        positionIndex: Int,
        title: String,
        subhead: String,
        sparksReward: Int
    ): Result<TaskDto> {
        return try {
            val insert = TaskDto(
                routineId = routineId,
                positionIndex = positionIndex,
                title = title,
                subhead = subhead,
                sparksReward = sparksReward
            )
            val created = provider.client.postgrest["tasks"]
                .insert(insert) {
                    select()
                }
                .decodeSingle<TaskDto>()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun update(
        taskId: String,
        title: String,
        subhead: String,
        sparksReward: Int
    ): Result<TaskDto> {
        return try {
            val patch = TaskUpdatePatch(
                title = title,
                subhead = subhead,
                sparksReward = sparksReward
            )
            val updated = provider.client.postgrest["tasks"]
                .update(patch) {
                    filter { eq("id", taskId) }
                    select()
                }
                .decodeSingle<TaskDto>()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(taskId: String): Result<Unit> {
        return try {
            provider.client.postgrest["tasks"].delete {
                filter { eq("id", taskId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
private data class TaskUpdatePatch(
    val title: String,
    val subhead: String,
    @SerialName("sparks_reward") val sparksReward: Int
)
