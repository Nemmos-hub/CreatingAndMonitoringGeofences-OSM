package com.google.android.gms.location.sample.geofencing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.Geofence

class GeofenceTransitionsWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val transition = inputData.getInt(
            GeofenceBroadcastReceiver.KEY_TRANSITION, -1)
        val ids = inputData.getStringArray(GeofenceBroadcastReceiver.KEY_IDS)
            ?: return Result.failure()

        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT) {
            return Result.failure()
        }

        val details = "${transitionString(transition)}: ${ids.joinToString(", ")}"
        sendNotification(details)
        return Result.success()
    }

    private fun sendNotification(details: String) {
        val ctx = applicationContext
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createChannelIfNeeded(nm, ctx)

        val stack = TaskStackBuilder.create(ctx).apply {
            addParentStack(MainActivity::class.java)
            addNextIntent(Intent(ctx, MainActivity::class.java))
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val pi = stack.getPendingIntent(0, flags)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(ctx.resources, R.mipmap.ic_launcher))
            .setColor(Color.RED)
            .setContentTitle(details)
            .setContentText(ctx.getString(R.string.geofence_transition_notification_text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannelIfNeeded(nm: NotificationManager, ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifiche transizioni geofence"
                enableLights(true)
                lightColor = Color.RED
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun transitionString(type: Int) = when (type) {
        Geofence.GEOFENCE_TRANSITION_ENTER ->
            applicationContext.getString(R.string.geofence_transition_entered)
        Geofence.GEOFENCE_TRANSITION_EXIT ->
            applicationContext.getString(R.string.geofence_transition_exited)
        else -> applicationContext.getString(R.string.unknown_geofence_transition)
    }

    companion object {
        private const val CHANNEL_ID = "geofence_channel_01"
        private const val NOTIFICATION_ID = 1
    }
}