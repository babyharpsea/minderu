package com.minderu.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class TaskRepository {

    private val db = SupabaseModule.client.postgrest

    /**
     * Serializes read-modify-write on the Sparks balance.
     *
     * Without this, two concurrent `toggleComplete` calls can both read the same
     * balance and the second write silently overwrites the first — losing Sparks.
     * A server-side RPC would be better, but the plan says "no schema changes".
     */
    private val sparksMutex = Mutex()

    // ── Routines ──────────────────────────────────────────────

    /** Fetch the first active routine for this user, or any existing routine as fallback, or null if none exist. */
    suspend fun fetchActiveRoutine(userId: String): RoutineDto? {
        val active = db["routines"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_active", true)
                }
            }
            .decodeList<RoutineDto>()
            .firstOrNull()

        if (active != null) return active

        return db["routines"]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<RoutineDto>()
            .firstOrNull()
    }

    /** Insert a "Daily Focus" routine with 3 seed tasks. Returns the new routine. */
    suspend fun createDefaultRoutine(userId: String): RoutineDto {
        val routine = RoutineDto(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = "Daily Focus",
            isActive = true
        )
        db["routines"].insert(routine)

        val seedTasks = listOf(
            TaskDto(UUID.randomUUID().toString(), routine.id, 0, "Put 3 clothes in your closet", "One tiny step", 3),
            TaskDto(UUID.randomUUID().toString(), routine.id, 1, "Drink a glass of water", "One tiny step", 2),
            TaskDto(UUID.randomUUID().toString(), routine.id, 2, "Clear one surface", "One tiny step", 3)
        )
        try {
            db["tasks"].insert(seedTasks)
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Seed tasks error: ${e.message}", e)
        }

        return routine
    }

    // ── Tasks ─────────────────────────────────────────────────

    /** Fetch all tasks for a routine, ordered by position_index. */
    suspend fun fetchTasksForRoutine(routineId: String): List<TaskDto> {
        return db["tasks"]
            .select {
                filter { eq("routine_id", routineId) }
                order("position_index", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList<TaskDto>()
    }

    /** Insert a new task with an explicit [positionIndex]. */
    suspend fun insertTask(dto: TaskDto) {
        db["tasks"].insert(dto)
    }

    /** Update title, subhead, and sparks_reward of an existing task. */
    suspend fun updateTask(taskId: String, title: String, subhead: String, sparksReward: Int) {
        db["tasks"].update({
            set("title", title)
            set("subhead", subhead)
            set("sparks_reward", sparksReward)
        }) {
            filter { eq("id", taskId) }
        }
    }

    /** Delete a task by id. */
    suspend fun deleteTask(taskId: String) {
        db["tasks"].delete {
            filter { eq("id", taskId) }
        }
    }

    // ── Profile / Sparks ──────────────────────────────────────

    /** Fetch the user's profile. */
    suspend fun fetchProfile(userId: String): ProfileDto? {
        return db["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeList<ProfileDto>()
            .firstOrNull()
    }

    /**
     * Ensure a `profiles` row exists for [userId].
     *
     * Checks if profile exists first to prevent overwriting existing sparks
     * or triggering upsert RLS failures.
     */
    suspend fun ensureProfile(userId: String) {
        try {
            val existing = fetchProfile(userId)
            if (existing == null) {
                db["profiles"].insert(
                    ProfileDto(id = userId, sparks = 0, isPremium = false)
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Ensure profile error: ${e.message}", e)
        }
    }

    /**
     * Adjust the Sparks balance by [delta] (positive to award, negative to deduct).
     *
     * Serialized through [sparksMutex] to prevent concurrent read-modify-write
     * from losing updates. Still best-effort — a failure doesn't crash the caller.
     *
     * @throws IllegalStateException if the profile doesn't exist (callers should
     *         [ensureProfile] during task loading).
     */
    suspend fun adjustSparks(userId: String, delta: Int) {
        sparksMutex.withLock {
            val profile = fetchProfile(userId)
                ?: throw IllegalStateException("Profile not found for user $userId")
            val newBalance = (profile.sparks + delta).coerceAtLeast(0)
            db["profiles"].update({
                set("sparks", newBalance)
            }) {
                filter { eq("id", userId) }
            }
        }
    }

    /** Update user profile streak data. */
    suspend fun updateProfileStreak(userId: String, currentStreak: Int, maxStreak: Int, lastActiveDate: String) {
        try {
            db["profiles"].update({
                set("current_streak", currentStreak)
                set("max_streak", maxStreak)
                set("last_active_date", lastActiveDate)
            }) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to update profile streak: ${e.message}", e)
        }
    }

    /** Log task completion into task_completions table. */
    suspend fun recordTaskCompletion(userId: String, taskId: String) {
        try {
            db["task_completions"].insert(
                TaskCompletionDto(userId = userId, taskId = taskId)
            )
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to log task completion: ${e.message}", e)
        }
    }
}
