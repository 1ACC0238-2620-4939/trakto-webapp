package pe.trako.traktoroute.features.tracking.data.remote

import pe.trako.traktoroute.features.tracking.domain.StopReason
import pe.trako.traktoroute.features.tracking.domain.Tracking
import pe.trako.traktoroute.features.tracking.domain.TrackingStatus
import pe.trako.traktoroute.features.tracking.domain.TrackingStop
import pe.trako.traktoroute.shared.data.remote.backendInstant
import pe.trako.traktoroute.shared.data.remote.requiredField
import pe.trako.traktoroute.shared.data.remote.backendCoordinates
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

internal interface TrackingApi {
    @GET("api/v1/trackings/trips/{tripId}")
    suspend fun findByTrip(@Path("tripId") tripId: String): TrackingResponse

    @POST("api/v1/trackings/{id}/positions")
    suspend fun sendPosition(@Path("id") id: String, @Body body: PositionRequest)

    @PATCH("api/v1/trackings/{id}/stops/{stopId}/reason")
    suspend fun changeStopReason(
        @Path("id") id: String,
        @Path("stopId") stopId: String,
        @Body body: StopReasonRequest
    )
}

internal data class PositionRequest(val latitude: Double, val longitude: Double, val recordedAt: String)
internal data class StopReasonRequest(val reason: String)

internal data class TrackingResponse(
    val trackingId: String?,
    val status: String?,
    val lastLatitude: Double?,
    val lastLongitude: Double?,
    val lastPositionRecordedAt: String?,
    val stops: List<TrackingStopResponse>?
) {
    fun toDomain(): Tracking = Tracking(
        id = trackingId.requiredField("trackingId"),
        status = TrackingStatus.entries.firstOrNull { it.name == status } ?: TrackingStatus.UNKNOWN,
        lastPosition = if (lastLatitude != null && lastLongitude != null) backendCoordinates(lastLatitude, lastLongitude) else null,
        lastRecordedAt = lastPositionRecordedAt?.backendInstant(),
        stops = stops.orEmpty().map(TrackingStopResponse::toDomain)
    )
}

internal data class TrackingStopResponse(
    val stopId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val startedAt: String?,
    val endedAt: String?,
    val reason: String?
) {
    fun toDomain(): TrackingStop = TrackingStop(
        id = stopId.requiredField("stopId"),
        coordinates = backendCoordinates(latitude.requiredField("latitude"), longitude.requiredField("longitude")),
        startedAt = startedAt.requiredField("startedAt").backendInstant(),
        endedAt = endedAt?.backendInstant(),
        reason = StopReason.entries.firstOrNull { it.name == reason } ?: StopReason.UNKNOWN
    )
}
