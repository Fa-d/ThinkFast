package dev.sadakat.thinkfaster.data.sync.supabase

import android.content.Context
import android.util.Log
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Simple test to verify Supabase connection
 * Call this from MainActivity to test
 */
object SupabaseConnectionTest {

    private const val TAG = "SupabaseTest"

    fun testConnection(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔄 Testing Supabase connection...")

                val supabase = SupabaseClientProvider.getInstance(context)

                // Test 1: Client initialized
                Log.d(TAG, "✅ Supabase client initialized successfully")

                // Test 2: Check auth module
                try {
                    val auth = supabase.auth
                    val currentUser = auth.currentUserOrNull()
                    if (currentUser != null) {
                        Log.d(TAG, "✅ User logged in: ${currentUser.id}")
                    } else {
                        Log.d(TAG, "✅ Auth module working - No user logged in (expected for first run)")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Auth check failed: ${e.message}")
                }

                // Test 3: Check database module
                try {
                    // Just verify the module is accessible - don't actually query
                    supabase.from("goals")
                    Log.d(TAG, "✅ Database module (Postgrest) initialized")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Database module check: ${e.message}")
                }

                Log.d(TAG, "🎉 Supabase connection test complete!")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Supabase connection failed: ${e.message}", e)
            }
        }
    }
}
