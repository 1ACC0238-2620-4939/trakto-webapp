package pe.trako.traktoroute.features.trips.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.trako.traktoroute.features.trips.data.local.TripDao
import pe.trako.traktoroute.features.trips.data.local.TripEntity
import pe.trako.traktoroute.features.trips.data.local.toEntity
import pe.trako.traktoroute.features.trips.data.remote.TripApi
import pe.trako.traktoroute.features.trips.data.remote.TripResponse
import pe.trako.traktoroute.features.trips.data.remote.toRequest
import pe.trako.traktoroute.features.trips.domain.Trip
import pe.trako.traktoroute.features.trips.domain.TripAction
import pe.trako.traktoroute.features.trips.domain.TripDraft
import pe.trako.traktoroute.features.trips.domain.TripRepository
import pe.trako.traktoroute.features.trips.domain.TripStatus
import pe.trako.traktoroute.shared.data.remote.backendCall
import pe.trako.traktoroute.shared.data.remote.requiredField
import pe.trako.traktoroute.shared.domain.BackendFailure
import java.time.Instant

internal class CachedTripRepository(
    private val api: TripApi,
    private val dao: TripDao
) : TripRepository {
    private val lock = Mutex()
    override val trips: Flow<List<Trip>> = dao.observe().map { rows ->
        rows.map(TripEntity::toDomain).sortedByDescending(Trip::scheduledAt)
    }

    override fun observeTrip(tripId: String): Flow<Trip?> =
        dao.observeTrip(tripId).map { it?.toDomain() }

    override suspend fun refresh() = lock.withLock {
        val trips = backendCall { api.list() }.map(TripResponse::toEntity)
        dao.replace(trips)
    }

    override suspend fun refreshTrip(tripId: String) = lock.withLock {
        try {
            val trip = backendCall { api.get(tripId) }.toEntity()
            if (trip.tripId != tripId) throw BackendFailure.InvalidResponse("Trip ID mismatch")
            dao.upsert(trip)
        } catch (error: BackendFailure.Http) {
            if (error.statusCode == 404) dao.deleteTrip(tripId)
            throw error
        }
    }

    override suspend fun create(draft: TripDraft): String = lock.withLock {
        val id = backendCall { api.create(draft.toRequest()) }.tripId.requiredField("tripId")
        val trip = Trip(
            id = id,
            driverId = draft.driverId,
            vehicleId = draft.vehicleId,
            origin = draft.origin,
            destination = draft.destination,
            status = TripStatus.SCHEDULED,
            scheduledAt = draft.scheduledAt
        )
        dao.upsert(trip.toEntity())
        id
    }

    override suspend fun changeStatus(tripId: String, action: TripAction) = lock.withLock {
        val trip = requireNotNull(dao.findTrip(tripId)) { "Actualiza el detalle antes de cambiar el estado." }.toDomain()
        val now = Instant.now()
        val updated = trip.after(action, now)
        val (operation, timestamp) = when (action) {
            TripAction.START -> "start" to "startedAt"
            TripAction.COMPLETE -> "complete" to "completedAt"
            TripAction.CANCEL -> "cancel" to "cancelledAt"
        }
        backendCall { api.changeStatus(tripId, operation, mapOf(timestamp to now.toString())) }
        dao.upsert(updated.toEntity())
    }
}
