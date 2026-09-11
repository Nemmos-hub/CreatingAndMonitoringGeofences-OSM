package com.google.android.gms.location.sample.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Evento geofence ricevuto")

        val event = GeofencingEvent.fromIntent(intent) ?: run {
            Log.e(TAG, "GeofencingEvent nullo")
            return
        }

        if (event.hasError()) {
            Log.e(TAG, "Errore geofencing: ${event.errorCode}")
            return
        }

        val ids = event.triggeringGeofences?.map { it.requestId }?.toTypedArray() ?: run {
            Log.e(TAG, "Nessuna geofence triggerata")
            return
        }

        val data = Data.Builder()
            .putInt(KEY_TRANSITION, event.geofenceTransition)
            .putStringArray(KEY_IDS, ids)
            .build()

        val work = OneTimeWorkRequestBuilder<GeofenceTransitionsWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(work)
        Log.d(TAG, "Worker accodato")
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val KEY_TRANSITION = "transition_type"
        const val KEY_IDS = "geofence_ids"
    }
}