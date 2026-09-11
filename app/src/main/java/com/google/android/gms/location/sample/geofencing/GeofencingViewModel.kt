package com.google.android.gms.location.sample.geofencing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class GeofencingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val _geofencesAdded = MutableLiveData<Boolean>()

    val geofencesAdded: LiveData<Boolean> = _geofencesAdded

    init {
        _geofencesAdded.value = prefs.getBoolean(Constants.GEOFENCES_ADDED_KEY, false)
    }

    fun areGeofencesAdded(): Boolean = _geofencesAdded.value == true

    fun setGeofencesAdded(added: Boolean) {
        prefs.edit().putBoolean(Constants.GEOFENCES_ADDED_KEY, added).apply()
        _geofencesAdded.value = added
    }
}