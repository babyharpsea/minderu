package com.minderu.data

import io.github.jan.supabase.postgrest.postgrest
import java.util.UUID

class TaskRepository {

    private val db = SupabaseModule.client.postgrest

    // ── Routines ──────────────────────────────────────────────

    /** Fetch the first active routine for this user, or null if none exist. */
    suspend fun fetchActiveRoutine(userId: String): RoutineDto? {
        return db["routines"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("is_active", true)
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
        db["tasks"].insert(seedTasks)

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

    /** Insert a new task. */
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

    /** Award sparks by adding a delta to the current balance (via RPC or read-modify-write). */
    suspend fun awardSparks(userId: String, delta: Int) {
        val profile = fetchProfile(userId) ?: return
        val newBalance = profile.sparks + delta
        db["profiles"].update({
            set("sparks", newBalance)
        }) {
            filter { eq("id", userId) }
        }
    }
}
