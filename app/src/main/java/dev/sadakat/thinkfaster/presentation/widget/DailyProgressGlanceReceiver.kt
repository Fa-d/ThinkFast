package dev.sadakat.thinkfaster.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Glance widget receiver - handles widget lifecycle
 *
 * This replaces the old AppWidgetProvider with Compose/Glance
 */
class DailyProgressGlanceReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = DailyProgressGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Called when first widget is added
        refreshWidgetData(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Called when last widget is removed
    }
}

/**
 * Action callback for refresh button
 *
 * Glance uses action callbacks instead of broadcast receivers
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Refresh data from database
        refreshWidgetData(context)

        // Update all widgets
        DailyProgressGlanceWidget().updateAll(context)
    }
}

/**
 * Helper function to trigger Glance widget update from anywhere in the app
 *
 * Use this instead of the old triggerWidgetUpdate() function
 */
suspend fun triggerGlanceWidgetUpdate(context: Context) {
    DailyProgressGlanceWidget().updateAll(context)
}

/**
 * Convenience function to trigger update in a coroutine
 */
fun triggerGlanceWidgetUpdateAsync(context: Context) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        triggerGlanceWidgetUpdate(context)
    }
}
