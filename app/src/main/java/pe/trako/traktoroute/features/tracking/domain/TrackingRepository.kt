package pe.trako.traktoroute.features.tracking.domain

import pe.trako.traktoroute.shared.domain.Coordinates
import java.time.Instant

internal interface TrackingRepository {
    suspend fun findByTrip(tripId: String): Tracking?
    suspend fun sendPosition(trackingId: String, coordinates: Coordinates, recordedAt: Instant)
    suspend fun changeStopReason(trackingId: String, stopId: String, reason: StopReason)
}
