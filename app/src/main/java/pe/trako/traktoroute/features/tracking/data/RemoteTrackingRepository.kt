package pe.trako.traktoroute.features.tracking.data

import pe.trako.traktoroute.features.tracking.data.remote.PositionRequest
import pe.trako.traktoroute.features.tracking.data.remote.StopReasonRequest
import pe.trako.traktoroute.features.tracking.data.remote.TrackingApi
import pe.trako.traktoroute.features.tracking.domain.StopReason
import pe.trako.traktoroute.features.tracking.domain.Tracking
import pe.trako.traktoroute.features.tracking.domain.TrackingRepository
import pe.trako.traktoroute.shared.data.remote.backendCall
import pe.trako.traktoroute.shared.domain.BackendFailure
import pe.trako.traktoroute.shared.domain.Coordinates
import java.time.Instant

internal class RemoteTrackingRepository(private val api: TrackingApi) : TrackingRepository {
    override suspend fun findByTrip(tripId: String): Tracking? = try {
        backendCall { api.findByTrip(tripId) }.toDomain()
    } catch (error: BackendFailure.Http) {
        if (error.statusCode == 404) null else throw error
    }

    override suspend fun sendPosition(trackingId: String, coordinates: Coordinates, recordedAt: Instant) {
        val body = PositionRequest(coordinates.latitude, coordinates.longitude, recordedAt.toString())
        backendCall { api.sendPosition(trackingId, body) }
    }

    override suspend fun changeStopReason(trackingId: String, stopId: String, reason: StopReason) =
        backendCall { api.changeStopReason(trackingId, stopId, StopReasonRequest(reason.name)) }
}
