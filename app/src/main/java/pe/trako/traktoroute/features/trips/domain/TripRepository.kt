package pe.trako.traktoroute.features.trips.domain

import kotlinx.coroutines.flow.Flow

internal interface TripRepository {
    val trips: Flow<List<Trip>>
    fun observeTrip(tripId: String): Flow<Trip?>
    suspend fun refresh()
    suspend fun refreshTrip(tripId: String)
    suspend fun create(draft: TripDraft): String
    suspend fun changeStatus(tripId: String, action: TripAction)
}
