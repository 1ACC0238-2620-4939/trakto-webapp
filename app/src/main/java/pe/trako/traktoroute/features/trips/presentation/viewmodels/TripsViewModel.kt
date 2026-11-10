package pe.trako.traktoroute.features.trips.presentation.viewmodels

import pe.trako.traktoroute.features.trips.domain.TripRepository
import pe.trako.traktoroute.shared.presentation.viewmodels.RequestViewModel

internal class TripsViewModel(private val repository: TripRepository) : RequestViewModel() {
    val trips = repository.trips

    fun refresh() = request { repository.refresh() }
}
