package pe.trako.traktoroute.features.tracking.data.location

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.trako.traktoroute.features.tracking.domain.GpsPhase
import pe.trako.traktoroute.features.tracking.domain.GpsSession
import pe.trako.traktoroute.features.tracking.domain.GpsTracker

internal class AndroidGpsTracker(private val context: Context) : GpsTracker {
    private val current = MutableStateFlow(GpsSession())
    override val session: StateFlow<GpsSession> = current.asStateFlow()

    override fun start(tripId: String, trackingId: String) {
        if (current.value.isRunning) return
        current.value = GpsSession(tripId, GpsPhase.STARTING)
        val intent = Intent(context, LocationTrackingService::class.java)
            .putExtra(LocationTrackingService.TRIP_ID, tripId)
            .putExtra(LocationTrackingService.TRACKING_ID, trackingId)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: IllegalStateException) {
            current.value = GpsSession(tripId, GpsPhase.UNAVAILABLE)
        } catch (_: SecurityException) {
            current.value = GpsSession(tripId, GpsPhase.UNAVAILABLE)
        }
    }

    override fun stop(tripId: String) {
        if (current.value.tripId == tripId && current.value.isRunning) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
            current.value = GpsSession()
        }
    }

    fun report(session: GpsSession) {
        if (current.value.tripId == session.tripId) current.value = session
    }

    fun onServiceStopped(tripId: String) {
        if (current.value.tripId == tripId && current.value.isRunning) current.value = GpsSession()
    }
}
