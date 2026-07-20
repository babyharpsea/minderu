package com.minderu.data

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.Realtime

object SupabaseConfig {
    const val URL = "https://omozxnmdptgeyxspgcro.supabase.co"
    const val KEY = "sb_publishable_4hZTucJQP1Zp2WJYObZKlQ_uo6cTfIh"
}

private val supabase = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.KEY
) {
    install(Postgrest)
    install(Auth)
    install(Realtime)
}

object SupabaseProvider {
    private const val TAG = "SupabaseProvider"

    val client = supabase

    suspend fun ensureAuthenticated(): Result<Unit> {
        return try {
            if (client.auth.currentSessionOrNull() == null) {
                client.auth.signInAnonymously()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            Result.failure(e)
        }
    }
}
