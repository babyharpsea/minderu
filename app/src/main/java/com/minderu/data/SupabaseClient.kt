package com.minderu.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime

object SupabaseModule {

    private const val URL = "https://omozxnmdptgeyxspgcro.supabase.co"
    private const val KEY = "sb_publishable_4hZTucJQP1Zp2WJYObZKlQ_uo6cTfIh"

    val client = createSupabaseClient(
        supabaseUrl = URL,
        supabaseKey = KEY
    ) {
        install(Postgrest)
        install(Auth) // SettingsSessionManager persists JWT automatically
        install(Realtime)
    }
}
