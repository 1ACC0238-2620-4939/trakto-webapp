package pe.trako.traktoroute.features.trips.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.trako.traktoroute.features.trips.domain.Trip
import pe.trako.traktoroute.features.trips.domain.TripStatus
import pe.trako.traktoroute.features.trips.domain.Waypoint
import pe.trako.traktoroute.shared.domain.Coordinates
import java.math.BigDecimal
import java.time.Instant

@Entity(tableName = "trips")
internal data class TripEntity(
    @PrimaryKey val tripId: String,
    val driverId: String,
    val vehicleId: String,
    val originAddress: String,
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationAddress: String,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val status: String,
    val scheduledAt: Instant,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val cancelledAt: Instant?,
    val distanceKm: BigDecimal?,
    val durationMinutes: Int?
) {
    fun toDomain(): Trip = Trip(
        id = tripId,
        driverId = driverId,
        vehicleId = vehicleId,
        origin = Waypoint(originAddress, Coordinates(originLatitude, originLongitude)),
        destination = Waypoint(destinationAddress, Coordinates(destinationLatitude, destinationLongitude)),
        status = TripStatus.entries.firstOrNull { it.name == status } ?: TripStatus.UNKNOWN,
        scheduledAt = scheduledAt,
        startedAt = startedAt,
        completedAt = completedAt,
        cancelledAt = cancelledAt,
        distanceKm = distanceKm,
        durationMinutes = durationMinutes
    )
}

internal fun Trip.toEntity(): TripEntity = TripEntity(
    tripId = id,
    driverId = driverId,
    vehicleId = vehicleId,
    originAddress = origin.address,
    originLatitude = origin.coordinates.latitude,
    originLongitude = origin.coordinates.longitude,
    destinationAddress = destination.address,
    destinationLatitude = destination.coordinates.latitude,
    destinationLongitude = destination.coordinates.longitude,
    status = status.name,
    scheduledAt = scheduledAt,
    startedAt = startedAt,
    completedAt = completedAt,
    cancelledAt = cancelledAt,
    distanceKm = distanceKm,
    durationMinutes = durationMinutes
)

@Dao
internal interface TripDao {
    @Query("SELECT * FROM trips")
    fun observe(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE tripId = :id")
    fun observeTrip(id: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE tripId = :id")
    suspend fun findTrip(id: String): TripEntity?

    @Query("DELETE FROM trips WHERE tripId = :id")
    suspend fun deleteTrip(id: String)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(trip: TripEntity)

    @Upsert
    suspend fun upsertAll(trips: List<TripEntity>)

    @Transaction
    suspend fun replace(trips: List<TripEntity>) {
        deleteAll()
        upsertAll(trips)
    }
}
