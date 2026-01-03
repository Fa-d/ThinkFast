// Supabase Edge Function for Account Deletion Requests
// Deploy: supabase functions deploy delete-account-request

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { email, reason } = await req.json()

    // Validate email
    if (!email || !email.includes('@')) {
      return new Response(
        JSON.stringify({ error: 'Valid email address is required' }),
        {
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

    // Create Supabase client with service role key (for admin operations)
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

    const supabase = createClient(supabaseUrl, supabaseServiceKey, {
      auth: {
        autoRefreshToken: false,
        persistSession: false
      }
    })

    console.log(`Account deletion request received for: ${email}`)

    // Log deletion request
    const { data: requestData, error: logError } = await supabase
      .from('deletion_requests')
      .insert({
        email: email.toLowerCase(),
        reason: reason || null,
        requested_at: new Date().toISOString(),
        status: 'pending',
        user_agent: req.headers.get('user-agent') || null
      })
      .select()
      .single()

    if (logError) {
      console.error('Error logging deletion request:', logError)
    }

    // Try to find user by email in auth.users
    const { data: { users }, error: getUserError } = await supabase.auth.admin.listUsers()

    if (getUserError) {
      console.error('Error fetching users:', getUserError)
      throw new Error('Failed to process deletion request')
    }

    const user = users?.find(u => u.email?.toLowerCase() === email.toLowerCase())

    if (user) {
      console.log(`User found: ${user.id}, proceeding with deletion`)

      try {
        // Delete all user data from tables
        // Order matters - delete child tables first to avoid foreign key constraints

        // 1. Delete usage events (references sessions)
        const { error: eventsError } = await supabase
          .from('usage_events')
          .delete()
          .eq('user_id', user.id)
        if (eventsError) console.error('Error deleting usage events:', eventsError)

        // 2. Delete intervention results (references sessions)
        const { error: interventionsError } = await supabase
          .from('intervention_results')
          .delete()
          .eq('user_id', user.id)
        if (interventionsError) console.error('Error deleting interventions:', interventionsError)

        // 3. Delete usage sessions
        const { error: sessionsError } = await supabase
          .from('usage_sessions')
          .delete()
          .eq('user_id', user.id)
        if (sessionsError) console.error('Error deleting sessions:', sessionsError)

        // 4. Delete daily stats
        const { error: statsError } = await supabase
          .from('daily_stats')
          .delete()
          .eq('user_id', user.id)
        if (statsError) console.error('Error deleting daily stats:', statsError)

        // 5. Delete streak recoveries
        const { error: recoveriesError } = await supabase
          .from('streak_recoveries')
          .delete()
          .eq('user_id', user.id)
        if (recoveriesError) console.error('Error deleting streak recoveries:', recoveriesError)

        // 6. Delete user baseline
        const { error: baselineError } = await supabase
          .from('user_baseline')
          .delete()
          .eq('user_id', user.id)
        if (baselineError) console.error('Error deleting user baseline:', baselineError)

        // 7. Delete goals
        const { error: goalsError } = await supabase
          .from('goals')
          .delete()
          .eq('user_id', user.id)
        if (goalsError) console.error('Error deleting goals:', goalsError)

        // 8. Delete settings
        const { error: settingsError } = await supabase
          .from('settings')
          .delete()
          .eq('user_id', user.id)
        if (settingsError) console.error('Error deleting settings:', settingsError)

        // 9. Delete user profile
        const { error: profileError } = await supabase
          .from('user_profiles')
          .delete()
          .eq('id', user.id)
        if (profileError) console.error('Error deleting user profile:', profileError)

        // 10. Delete auth user
        const { error: deleteUserError } = await supabase.auth.admin.deleteUser(user.id)
        if (deleteUserError) {
          console.error('Error deleting auth user:', deleteUserError)
          throw new Error('Failed to delete user account')
        }

        // Update deletion request status
        if (requestData) {
          await supabase
            .from('deletion_requests')
            .update({
              status: 'completed',
              completed_at: new Date().toISOString(),
              user_id: user.id
            })
            .eq('id', requestData.id)
        }

        console.log(`Successfully deleted account for: ${email}`)

        return new Response(
          JSON.stringify({
            success: true,
            message: 'Account deletion completed',
            email: email
          }),
          {
            status: 200,
            headers: { ...corsHeaders, 'Content-Type': 'application/json' }
          }
        )

      } catch (deleteError) {
        console.error('Error during deletion:', deleteError)

        // Update request status to error
        if (requestData) {
          await supabase
            .from('deletion_requests')
            .update({
              status: 'error',
              error_message: deleteError.message
            })
            .eq('id', requestData.id)
        }

        throw deleteError
      }

    } else {
      console.log(`No account found for email: ${email}`)

      // Update request status
      if (requestData) {
        await supabase
          .from('deletion_requests')
          .update({
            status: 'user_not_found',
            note: 'No account found with this email address'
          })
          .eq('id', requestData.id)
      }

      // Still return success to the user (privacy-friendly)
      return new Response(
        JSON.stringify({
          success: true,
          message: 'Deletion request received',
          email: email
        }),
        {
          status: 200,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

  } catch (error) {
    console.error('Unexpected error:', error)

    return new Response(
      JSON.stringify({
        error: error.message || 'Internal server error',
        success: false
      }),
      {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    )
  }
})
