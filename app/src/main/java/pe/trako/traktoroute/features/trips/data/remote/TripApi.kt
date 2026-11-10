package pe.trako.traktoroute.features.trips.data.remote

import pe.trako.traktoroute.features.trips.data.local.TripEntity
import pe.trako.traktoroute.features.trips.domain.TripDraft
import pe.trako.traktoroute.shared.data.remote.backendInstant
import pe.trako.traktoroute.shared.data.remote.requiredField
import pe.trako.traktoroute.shared.domain.BackendFailure
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigDecimal

internal interface TripApi {
    @GET("api/v1/trips")
    suspend fun list(): List<TripResponse>

    @GET("api/v1/trips/{id}")
    suspend fun get(@Path("id") id: String): TripResponse

    @POST("api/v1/trips")
    suspend fun create(@Body body: CreateTripRequest): TripCreatedResponse

    @PATCH("api/v1/trips/{id}/{operation}")
    suspend fun changeStatus(
        @Path("id") id: String,
        @Path("operation") operation: String,
        @Body body: Map<String, String>
    )
}

internal data class TripCreatedResponse(val tripId: String?)

internal data class CreateTripRequest(
    val driverId: String,
    val vehicleId: String,
    val originAddress: String,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationAddress: String,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val scheduledAt: String
)

internal fun TripDraft.toRequest(): CreateTripRequest = CreateTripRequest(
    driverId = driverId,
    vehicleId = vehicleId,
    originAddress = origin.address,
    originLatitude = origin.coordinates.latitude,
    originLongitude = origin.coordinates.longitude,
    destinationAddress = destination.address,
    destinationLatitude = destination.coordinates.latitude,
    destinationLongitude = destination.coordinates.longitude,
    scheduledAt = scheduledAt.toString()
)

internal data class TripResponse(
    val tripId: String?,
    val driverId: String?,
    val vehicleId: String?,
    val originAddress: String?,
    val originLatitude: Double?,
    val originLongitude: Double?,
    val destinationAddress: String?,
    val destinationLatitude: Double?,
    val destinationLongitude: Double?,
    val status: String?,
    val scheduledAt: String?,
    val startedAt: String?,
    val completedAt: String?,
    val cancelledAt: String?,
    val distanceKm: BigDecimal?,
    val durationMinutes: Int?
) {
    fun toEntity(): TripEntity = TripEntity(
        tripId = tripId.requiredField("tripId"),
        driverId = driverId.requiredField("driverId"),
        vehicleId = vehicleId.requiredField("vehicleId"),
        originAddress = originAddress.requiredField("originAddress"),
        originLatitude = originLatitude.requiredField("originLatitude"),
        originLongitude = originLongitude.requiredField("originLongitude"),
        destinationAddress = destinationAddress.requiredField("destinationAddress"),
        destinationLatitude = destinationLatitude.requiredField("destinationLatitude"),
        destinationLongitude = destinationLongitude.requiredField("destinationLongitude"),
        status = status.requiredField("status"),
        scheduledAt = scheduledAt.requiredField("scheduledAt").backendInstant(),
        startedAt = startedAt?.backendInstant(),
        completedAt = completedAt?.backendInstant(),
        cancelledAt = cancelledAt?.backendInstant(),
        distanceKm = distanceKm,
        durationMinutes = durationMinutes
    ).also { trip ->
        try {
            trip.toDomain()
        } catch (error: IllegalArgumentException) {
            throw BackendFailure.InvalidResponse("Invalid trip location", error)
        }
    }
}
