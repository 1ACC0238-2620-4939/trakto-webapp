package pe.trako.traktoroute.features.tracking.domain

import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

internal interface GpsTracker {
    val session: StateFlow<GpsSession>
    fun start(tripId: String, trackingId: String)
    fun stop(tripId: String)
}

internal data class GpsSession(
    val tripId: String? = null,
    val phase: GpsPhase = GpsPhase.IDLE,
    val lastSentAt: Instant? = null
) {
    val isRunning: Boolean
        get() = when (phase) {
            GpsPhase.STARTING, GpsPhase.SEARCHING, GpsPhase.SHARING, GpsPhase.RETRYING -> true
            GpsPhase.IDLE, GpsPhase.UNAVAILABLE, GpsPhase.FINISHED -> false
        }
}

internal enum class GpsPhase { IDLE, STARTING, SEARCHING, SHARING, RETRYING, UNAVAILABLE, FINISHED }
