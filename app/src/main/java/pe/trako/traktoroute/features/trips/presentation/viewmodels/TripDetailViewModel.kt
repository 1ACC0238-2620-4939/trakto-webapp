package pe.trako.traktoroute.features.trips.presentation.viewmodels

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.trako.traktoroute.features.drivers.domain.DriverRepository
import pe.trako.traktoroute.features.profiles.domain.ProfileRepository
import pe.trako.traktoroute.features.tracking.domain.GpsTracker
import pe.trako.traktoroute.features.trips.domain.Trip
import pe.trako.traktoroute.features.trips.domain.TripAction
import pe.trako.traktoroute.features.trips.domain.TripRepository
import pe.trako.traktoroute.features.vehicles.domain.VehicleRepository
import pe.trako.traktoroute.shared.presentation.viewmodels.RequestViewModel
import pe.trako.traktoroute.shared.presentation.viewmodels.refreshTogether

internal data class TripDetail(
    val trip: Trip? = null,
    val driverLabel: String = "",
    val vehicleLabel: String = ""
)

internal class TripDetailViewModel(
    private val tripId: String,
    private val trips: TripRepository,
    private val drivers: DriverRepository,
    private val vehicles: VehicleRepository,
    private val profiles: ProfileRepository,
    private val gps: GpsTracker
) : RequestViewModel() {
    val detail = combine(trips.observeTrip(tripId), drivers.drivers, vehicles.vehicles, profiles.profiles) {
            trip, currentDrivers, currentVehicles, currentProfiles ->
        if (trip == null) {
            TripDetail()
        } else {
            val driver = currentDrivers.find { it.id == trip.driverId }
            val name = driver?.let { selected -> currentProfiles.find { it.id == selected.profileId }?.fullName }
            TripDetail(
                trip = trip,
                driverLabel = name ?: driver?.licenseNumber ?: trip.driverId,
                vehicleLabel = currentVehicles.find { it.id == trip.vehicleId }?.displayPlate ?: trip.vehicleId
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetail())

    private val mutableVerified = MutableStateFlow(false)
    val verified = mutableVerified.asStateFlow()

    fun refresh() = request {
        mutableVerified.value = false
        trips.refreshTrip(tripId)
        mutableVerified.value = true
        loadAssignment()
    }

    fun changeStatus(action: TripAction) = request {
        require(mutableVerified.value) { "Actualiza el viaje antes de cambiar su estado." }
        trips.changeStatus(tripId, action)
        if (action == TripAction.COMPLETE || action == TripAction.CANCEL) gps.stop(tripId)
    }

    private fun loadAssignment() {
        viewModelScope.launch {
            refreshTogether(
                { if (drivers.drivers.first().isEmpty()) drivers.refresh() },
                { if (vehicles.vehicles.first().isEmpty()) vehicles.refresh() },
                { if (profiles.profiles.first().isEmpty()) profiles.refresh() }
            )
        }
    }
}
