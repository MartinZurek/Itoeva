package com.notime.glyphsim.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.notime.glyphsim.R
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphsim.matrix.ClockFrameSim
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.MatrixBitmapRenderer
import com.notime.glyphsim.ui.ClockStylePrefs
import com.notime.glyphsim.ui.MainActivity
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Rundes Home-Screen-Widget mit der simulierten Glyph-Matrix: zeigt dieselbe
 * Uhr wie im Sim-Screen ([ClockFrameSim]) und blendet bei einer faelligen
 * Erinnerung kurz deren Animation ein (siehe [playReminderAnimation], von
 * reminder/ReminderAlarmReceiver.kt aufgerufen).
 *
 * Minuetliche Aktualisierung laeuft nicht ueber updatePeriodMillis (Minimum
 * 30min, siehe glyph_widget_info.xml), sondern ueber selbst nachgelegte exakte
 * Alarme (gleiches Muster wie reminder/ReminderScheduler.kt) - App-Widgets
 * bekommen sonst keinen minuetlichen Callback.
 */
class GlyphClockWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TICK) {
            updateAllClocks(context)
            scheduleNextTick(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val frame = ClockFrameSim.buildFrame(style = ClockStylePrefs.get(context))
        appWidgetIds.forEach { pushFrameTo(context, appWidgetManager, it, frame) }
        scheduleNextTick(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        pushFrameTo(context, appWidgetManager, appWidgetId, ClockFrameSim.buildFrame(style = ClockStylePrefs.get(context)))
    }

    override fun onEnabled(context: Context) {
        scheduleNextTick(context)
    }

    override fun onDisabled(context: Context) {
        alarmManager(context)?.cancel(tickPendingIntent(context))
    }

    companion object {
        const val ACTION_TICK = "com.notime.glyphsim.widget.ACTION_TICK"

        /**
         * Eigener Scope fuer die Widget-Animation. Er ueberlebt bewusst den Aufrufer: der Alarm-
         * Receiver ist nach wenigen Millisekunden fertig, die Animation laeuft laenger.
         * SupervisorJob, damit ein Fehler in einer Wiedergabe den Scope nicht dauerhaft
         * unbrauchbar macht.
         */
        private val animationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var animationJob: Job? = null

        private fun widgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, GlyphClockWidgetProvider::class.java))

        fun hasActiveWidgets(context: Context): Boolean = widgetIds(context).isNotEmpty()

        fun updateAllClocks(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val frame = ClockFrameSim.buildFrame(style = ClockStylePrefs.get(context))
            widgetIds(context).forEach { pushFrameTo(context, manager, it, frame) }
        }

        /**
         * Spielt eine schon aufgeloeste Frame-Liste (fest eingebaut oder aus der Bibliothek) auf
         * allen Widget-Instanzen ab, dann zurueck zur Uhr.
         *
         * [openDurationSeconds] ist die pro Erinnerung eingestellte Offen-Dauer (siehe
         * [ReminderOpenDuration]), hier aber bewusst auf [MAX_WIDGET_DURATION_SECONDS] gedeckelt:
         * auf dem Home-Screen-Widget gibt es keinen Avatar zum Fuettern, eine lange oder gar
         * endlose ("Nonstop") Animation liesse sich dort also gar nicht beenden und wuerde
         * dauerhaft RemoteViews-Updates erzeugen. Die volle Dauer inklusive "Nonstop" gilt im
         * Dock-Modus, wo das Fuettern sie jederzeit abbricht (siehe ui/DockScreen.kt).
         */
        /**
         * Laeuft in einem eigenen Scope statt im Scope des Aufrufers - aus zwei Gruenden.
         *
         * Erstens: Der Aufrufer ([com.notime.glyphsim.reminder.ReminderTrigger]) wuerde sonst bis
         * zum Ende der Wiedergabe blockieren, also bis zu einer Minute. Die In-App-Anzeige haengt
         * am [com.notime.glyphsim.matrix.ReminderAnimationBus], der erst danach benachrichtigt
         * wuerde - die Erinnerung erschiene in der App also erst, wenn sie im Widget schon vorbei
         * ist.
         *
         * Zweitens: So laesst sich die Wiedergabe von aussen beenden, siehe
         * [stopReminderAnimation]. Ohne das lief die Animation im Widget weiter, nachdem der
         * Nutzer die Erinnerung in der App bereits gefuettert hatte.
         */
        fun playReminderAnimation(
            context: Context,
            frames: List<IntArray>,
            openDurationSeconds: Int = ReminderOpenDuration.DEFAULT_SECONDS
        ) {
            if (!hasActiveWidgets(context)) return
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val cappedSeconds = if (openDurationSeconds == ReminderOpenDuration.UNTIL_FED) {
                MAX_WIDGET_DURATION_SECONDS
            } else {
                openDurationSeconds.coerceAtMost(MAX_WIDGET_DURATION_SECONDS)
            }
            animationJob?.cancel()
            animationJob = animationScope.launch {
                MatrixAnimator.play(frames, targetDurationMs = cappedSeconds * 1000L) { frame ->
                    widgetIds(appContext).forEach { pushFrameTo(appContext, manager, it, frame) }
                }
                // Regulaeres Ende. Beim Abbruch stellt stopReminderAnimation die Uhr wieder her -
                // hier wuerde es nicht mehr laufen, weil die Coroutine dann schon beendet ist.
                updateAllClocks(appContext)
            }
        }

        /**
         * Bricht eine laufende Erinnerungs-Animation ab und stellt die Uhr wieder her.
         *
         * Wird beim Fuettern aufgerufen (siehe [com.notime.glyphsim.ui.AvatarFeeding]): Wer die
         * Erinnerung im Widget sieht, die App oeffnet und dort fuettert, hat sie beantwortet - im
         * Widget muss sie dann ebenfalls verschwinden. Sonst stuende dort weiter eine Animation,
         * die zu keinem offenen Auftreten mehr gehoert, und liesse sich scheinbar erneut
         * fuettern.
         */
        fun stopReminderAnimation(context: Context) {
            if (animationJob == null) return
            animationJob?.cancel()
            animationJob = null
            updateAllClocks(context.applicationContext)
        }

        private fun pushFrameTo(context: Context, manager: AppWidgetManager, appWidgetId: Int, frame: IntArray) {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 96)
                .coerceAtLeast(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 96))
                .coerceAtLeast(48)
            val density = context.resources.displayMetrics.density
            val sizePx = (widthDp * density).toInt().coerceAtLeast(64)

            val views = RemoteViews(context.packageName, R.layout.glyph_widget)
            views.setImageViewBitmap(R.id.imageClock, MatrixBitmapRenderer.render(frame, sizePx))

            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.imageClock, openAppPendingIntent)

            manager.updateAppWidget(appWidgetId, views)
        }

        private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

        private fun tickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, GlyphClockWidgetProvider::class.java).apply { action = ACTION_TICK }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun canScheduleExact(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            return alarmManager(context)?.canScheduleExactAlarms() ?: false
        }

        fun scheduleNextTick(context: Context) {
            if (!hasActiveWidgets(context)) return
            val manager = alarmManager(context) ?: return
            val nextMinute = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val pendingIntent = tickPendingIntent(context)
            try {
                if (canScheduleExact(context)) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
                } else {
                    // Der Regelfall seit dem Verzicht auf SCHEDULE_EXACT_ALARM (Begruendung im
                    // Manifest): die Uhr tickt ungefaehr statt minutengenau. Sichtbar ist das
                    // Widget ohnehin nur bei eingeschaltetem Display - und dann greift Doze
                    // nicht, der Tick kommt also nah am vollen Minutenwechsel.
                    Log.d(TAG, "Widget-Tick laeuft ungefaehr (keine Exact-Alarm-Berechtigung)")
                    manager.setAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pendingIntent)
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Widget-Tick konnte nicht geplant werden", e)
            }
        }

        private const val TAG = "GlyphClockWidget"

        /** Siehe [playReminderAnimation] - Obergrenze, weil am Widget nicht gefuettert werden kann. */
        private const val MAX_WIDGET_DURATION_SECONDS = 60
    }
}
