package dev.sadakat.thinkfaster.data.sync.supabase

import android.content.Context
import dev.sadakat.thinkfaster.R
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * Supabase client singleton
 * Configured with Postgrest (database) and GoTrue (auth)
 *
 * Uses a custom JSON serializer that ignores unknown keys (like created_at)
 * to allow Supabase's automatic timestamp columns without defining them in models
 */
object SupabaseClientProvider {

    private var instance: SupabaseClient? = null

    fun getInstance(context: Context): SupabaseClient {
        return instance ?: synchronized(this) {
            instance ?: createClient(context).also { instance = it }
        }
    }

    private fun createClient(context: Context): SupabaseClient {
        val supabaseUrl = context.getString(R.string.supabase_url)
        val supabaseKey = context.getString(R.string.supabase_anon_key)

        // Create a JSON serializer that ignores unknown keys
        // This allows Supabase's automatic columns (created_at, updated_at, etc.)
        // without requiring them in our data models
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Postgrest) {
                serializer = KotlinXSerializer(json)
            }
            install(Auth)
        }
    }
}
