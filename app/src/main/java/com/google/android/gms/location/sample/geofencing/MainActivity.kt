package com.google.android.gms.location.sample.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import java.io.File

class MainActivity : AppCompatActivity() {

    private enum class PendingGeofenceTask { ADD, REMOVE, NONE }

    private val viewModel: GeofencingViewModel by viewModels()

    private lateinit var geofencingClient: GeofencingClient
    private val geofenceList = mutableListOf<Geofence>()
    private var geofencePendingIntent: PendingIntent? = null

    private lateinit var addGeofencesButton: Button
    private lateinit var removeGeofencesButton: Button
    private lateinit var mapView: MapView

    private var pendingTask = PendingGeofenceTask.NONE

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) performPendingTask() else showPermissionDeniedSnackbar()
        }

    private val requestBackgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) performPendingTask()
            else showSnackbar("Background location richiesto per geofencing")
        }

    private val requestNotificationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupOsmdroid()
        setContentView(R.layout.activity_main)

        addGeofencesButton = findViewById(R.id.add_geofences_button)
        removeGeofencesButton = findViewById(R.id.remove_geofences_button)
        mapView = findViewById(R.id.map)

        setupMap()
        geofencingClient = LocationServices.getGeofencingClient(this)
        populateGeofenceList()
        drawGeofencesOnMap()

        viewModel.geofencesAdded.observe(this) { updateButtonsState(it) }
        updateButtonsState(viewModel.areGeofencesAdded())

        maybeRequestNotificationPermission()
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause()   { super.onPause();  mapView.onPause()   }

    // ========== SETUP ==========

    private fun setupOsmdroid() {
        Configuration.getInstance().apply {
            userAgentValue = packageName
            val cache = File(this@MainActivity.cacheDir, "osmdroid").apply { mkdirs() }
            osmdroidBasePath = cache
            osmdroidTileCache = cache
        }
    }

    private fun setupMap() {
        mapView.apply {
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            Constants.BAY_AREA_LANDMARKS.values.firstOrNull()?.let {
                controller.setCenter(it)
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ========== PERMESSI ==========

    private fun checkPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bg = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            fine && bg
        } else fine
    }

    private fun requestPermissions() {
        when {
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED -> {
                showBackgroundPermissionRationale()
            }
        }
    }

    private fun showBackgroundPermissionRationale() {
        Snackbar.make(findViewById(android.R.id.content),
            "Background location necessario per geofencing in background",
            Snackbar.LENGTH_LONG)
            .setAction("CONSENTI") {
                requestBackgroundLocationLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }.show()
    }

    private fun showPermissionDeniedSnackbar() {
        Snackbar.make(findViewById(android.R.id.content),
            "Permesso location necessario", Snackbar.LENGTH_INDEFINITE)
            .setAction("IMPOSTAZIONI") { openAppSettings() }.show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    // ========== HANDLER PULSANTI ==========

    fun addGeofencesButtonHandler(view: View) {
        if (!checkPermissions()) {
            pendingTask = PendingGeofenceTask.ADD
            requestPermissions()
            return
        }
        addGeofences()
    }

    fun removeGeofencesButtonHandler(view: View) {
        if (!checkPermissions()) {
            pendingTask = PendingGeofenceTask.REMOVE
            requestPermissions()
            return
        }
        removeGeofences()
    }

    @Suppress("MissingPermission")
    private fun addGeofences() {
        if (!checkPermissions()) { showSnackbar("Permessi insufficienti"); return }
        geofencingClient
            .addGeofences(buildGeofencingRequest(), geofencePendingIntent())
            .addOnCompleteListener { handleTaskResult(it) }
    }

    @Suppress("MissingPermission")
    private fun removeGeofences() {
        if (!checkPermissions()) { showSnackbar("Permessi insufficienti"); return }
        geofencingClient
            .removeGeofences(geofencePendingIntent())
            .addOnCompleteListener { handleTaskResult(it) }
    }

    private fun handleTaskResult(task: Task<Void>) {
        pendingTask = PendingGeofenceTask.NONE
        if (task.isSuccessful) {
            val added = !viewModel.areGeofencesAdded()
            viewModel.setGeofencesAdded(added)
            Toast.makeText(this,
                getString(if (added) R.string.geofences_added else R.string.geofences_removed),
                Toast.LENGTH_SHORT).show()
        } else {
            val msg = GeofenceErrorMessages.getErrorString(this, task.exception)
            Log.w(TAG, msg)
            showSnackbar(msg)
        }
    }

    // ========== GEOFENCING ==========

    private fun buildGeofencingRequest(): GeofencingRequest =
        GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

    private fun geofencePendingIntent(): PendingIntent {
        geofencePendingIntent?.let { return it }
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(this, 0, intent, flags).also {
            geofencePendingIntent = it
        }
    }

    private fun populateGeofenceList() {
        geofenceList.clear()
        Constants.BAY_AREA_LANDMARKS.forEach { (id, point) ->
            geofenceList += Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(
                    point.latitude,
                    point.longitude,
                    Constants.GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }
    }

    private fun drawGeofencesOnMap() {
        mapView.overlays.clear()
        Constants.BAY_AREA_LANDMARKS.forEach { (id, point: GeoPoint) ->
            val circle = Polygon().apply {
                setPoints(Polygon.pointsAsCircle(
                    point, Constants.GEOFENCE_RADIUS_IN_METERS.toDouble()))
                fillColor = 0x220000FF.toInt()
                strokeColor = 0xFF0000FF.toInt()
                strokeWidth = 2.0f
                title = id
            }
            mapView.overlays += circle
        }
        mapView.invalidate()
    }

    // ========== UTILITY ==========

    private fun updateButtonsState(added: Boolean) {
        addGeofencesButton.isEnabled = !added
        removeGeofencesButton.isEnabled = added
    }

    private fun performPendingTask() {
        when (pendingTask) {
            PendingGeofenceTask.ADD    -> addGeofences()
            PendingGeofenceTask.REMOVE -> removeGeofences()
            PendingGeofenceTask.NONE   -> Unit
        }
    }

    private fun showSnackbar(message: String) {
        findViewById<View>(android.R.id.content)?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }

    companion object { private const val TAG = "GeofencingDemo" }
}