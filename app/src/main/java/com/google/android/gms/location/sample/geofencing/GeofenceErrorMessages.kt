package com.google.android.gms.location.sample.geofencing

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes

object GeofenceErrorMessages {

    fun getErrorString(context: Context, e: Exception?): String =
        when (e) {
            is ApiException -> getErrorString(context, e.statusCode)
            else -> context.getString(R.string.unknown_geofence_error)
        }

    fun getErrorString(context: Context, code: Int): String = when (code) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
            context.getString(R.string.geofence_not_available)
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
            context.getString(R.string.geofence_too_many_geofences)
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
            context.getString(R.string.geofence_too_many_pending_intents)
        else -> context.getString(R.string.unknown_geofence_error)
    }
}