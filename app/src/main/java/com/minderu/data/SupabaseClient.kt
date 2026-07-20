package com.minderu.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.Realtime

object SupabaseConfig {
    const val URL = "https://omozxnmdptgeyxspgcro.supabase.co"
    const val KEY = "sb_publishable_4hZTucJQP1Zp2WJYObZKlQ_uo6cTfIh"
}

val supabase = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.KEY
) {
    install(Postgrest)
    install(Auth)
    install(Realtime)
}

object SupabaseProvider {
    val client = supabase

    suspend fun ensureAuthenticated() {
        val session = client.auth.currentSessionOrNull()
        if (session == null) {
            client.auth.signInAnonymously()
        }
    }
}
