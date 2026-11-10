package pe.trako.traktoroute.features.tracking.data.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import pe.trako.traktoroute.MainActivity
import pe.trako.traktoroute.TraktoApplication
import pe.trako.traktoroute.features.tracking.domain.GpsPhase
import pe.trako.traktoroute.features.tracking.domain.GpsSession
import pe.trako.traktoroute.features.tracking.domain.TrackingStatus
import pe.trako.traktoroute.shared.domain.BackendFailure
import pe.trako.traktoroute.shared.domain.Coordinates
import java.time.Instant

class LocationTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container get() = (application as TraktoApplication).container
    private val manager: LocationManager by lazy { getSystemService(LocationManager::class.java) }
    private var listener: LocationListener? = null
    private var tripInService: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == PAUSE) {
            if (intent.getStringExtra(TRIP_ID) == tripInService) stopSelf()
            return START_NOT_STICKY
        }
        val tripId = intent?.getStringExtra(TRIP_ID)
        val trackingId = intent?.getStringExtra(TRACKING_ID)
        if (tripId == null || trackingId == null || container.gpsTracker.session.value.tripId != tripId) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (tripInService != null) return START_NOT_STICKY
        tripInService = tripId

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(tripId), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            startUpdates(tripId, trackingId)
        } catch (_: RuntimeException) {
            container.gpsTracker.report(GpsSession(tripId, GpsPhase.UNAVAILABLE))
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startUpdates(tripId: String, trackingId: String) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Precise location permission is required")
        }
        check(manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        val locations = Channel<Location>(Channel.CONFLATED)
        val callback = LocationListener { location -> locations.trySend(location) }
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15_000L, 0f, callback, mainLooper)
        listener = callback
        container.gpsTracker.report(GpsSession(tripId, GpsPhase.SEARCHING))

        scope.launch {
            var lastSentAt: Instant? = null
            for (location in locations) {
                val recordedAt = Instant.ofEpochMilli(location.time)
                if (lastSentAt != null && !recordedAt.isAfter(lastSentAt)) continue

                try {
                    val tracking = container.tracking.findByTrip(tripId)
                    if (tracking == null || tracking.id != trackingId || tracking.status != TrackingStatus.ACTIVE) {
                        container.gpsTracker.report(GpsSession(tripId, GpsPhase.FINISHED))
                        stopSelf()
                        break
                    }
                    if (tracking.lastRecordedAt?.let { !recordedAt.isAfter(it) } == true) continue
                    val coordinates = Coordinates(location.latitude, location.longitude)
                    container.tracking.sendPosition(trackingId, coordinates, recordedAt)
                    lastSentAt = recordedAt
                    container.gpsTracker.report(GpsSession(tripId, GpsPhase.SHARING, recordedAt))
                } catch (_: BackendFailure) {
                    container.gpsTracker.report(GpsSession(tripId, GpsPhase.RETRYING, lastSentAt))
                }
            }
        }
    }

    private fun notification(tripId: String): Notification {
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel("trakto_gps", "Seguimiento GPS", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val pause = PendingIntent.getService(
            this,
            1,
            Intent(this, LocationTrackingService::class.java).setAction(PAUSE).putExtra(TRIP_ID, tripId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, "trakto_gps")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Trakto Route · GPS activo")
            .setContentText("Enviando la ubicación de este dispositivo para el viaje.")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Pausar GPS", pause).build())
            .build()
    }

    override fun onDestroy() {
        listener?.let(manager::removeUpdates)
        scope.cancel()
        tripInService?.let(container.gpsTracker::onServiceStopped)
        super.onDestroy()
    }

    internal companion object {
        const val TRIP_ID = "tripId"
        const val TRACKING_ID = "trackingId"
        private const val PAUSE = "pe.trako.traktoroute.PAUSE_GPS"
        private const val NOTIFICATION_ID = 100
    }
}
