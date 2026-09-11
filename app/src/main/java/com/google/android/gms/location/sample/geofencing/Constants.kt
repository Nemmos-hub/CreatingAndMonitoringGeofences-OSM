package com.google.android.gms.location.sample.geofencing

import org.osmdroid.util.GeoPoint

/**
 * Costanti del progetto demo.
 *
 * ATTENZIONE: attualmente i dati delle geofence sono hardcoded.
 * In futuro verranno recuperati da Supabase tramite chiamata di rete.
 */
object Constants {

    const val GEOFENCES_ADDED_KEY = "geofences_added"

    private const val GEOFENCE_EXPIRATION_IN_HOURS = 24L
    const val GEOFENCE_EXPIRATION_IN_MILLISECONDS =
        GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000

    const val GEOFENCE_RADIUS_IN_METERS = 500f

    /** Sostituibile con dati remoti (Supabase) senza cambiare il resto del codice. */
    val BAY_AREA_LANDMARKS: Map<String, GeoPoint> = mapOf(
        "SFO"          to GeoPoint(37.621313, -122.378955),
        "GOOGLE"       to GeoPoint(37.422611, -122.0840577),
        "GOLDEN_GATE"  to GeoPoint(37.819722, -122.478611),
        "STANFORD"     to GeoPoint(37.427474, -122.169719)
    )
}