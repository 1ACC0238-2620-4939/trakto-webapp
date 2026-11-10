package pe.trako.traktoroute.features.drivers.presentation.viewmodels

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.trako.traktoroute.features.drivers.domain.Driver
import pe.trako.traktoroute.features.drivers.domain.DriverRepository
import pe.trako.traktoroute.features.drivers.domain.DriverStatus
import pe.trako.traktoroute.features.profiles.domain.Profile
import pe.trako.traktoroute.features.profiles.domain.ProfileRepository
import pe.trako.traktoroute.shared.presentation.viewmodels.RequestViewModel
import pe.trako.traktoroute.shared.presentation.viewmodels.refreshTogether

internal data class DriverDirectory(
    val drivers: List<Driver> = emptyList(),
    val profiles: Map<String, Profile> = emptyMap(),
    val availableProfiles: List<Profile> = emptyList()
)

internal class DriversViewModel(
    private val drivers: DriverRepository,
    private val profiles: ProfileRepository
) : RequestViewModel() {
    val directory = combine(drivers.drivers, profiles.profiles) { currentDrivers, currentProfiles ->
        val assignedProfiles = currentDrivers.mapTo(mutableSetOf(), Driver::profileId)
        DriverDirectory(
            drivers = currentDrivers,
            profiles = currentProfiles.associateBy(Profile::id),
            availableProfiles = currentProfiles.filterNot { it.id in assignedProfiles }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DriverDirectory())

    fun refresh() = request {
        refreshTogether(drivers::refresh, profiles::refresh).firstOrNull()?.let { throw it }
    }

    fun create(profileId: String, licenseNumber: String) = request(save = true) {
        require(directory.value.availableProfiles.any { it.id == profileId }) {
            "Selecciona un perfil existente que todavía no tenga un conductor."
        }
        drivers.create(profileId, licenseNumber)
    }

    fun updateLicense(driverId: String, licenseNumber: String) = request(save = true) {
        drivers.updateLicense(driverId, licenseNumber)
    }

    fun toggleActive(driver: Driver) = request {
        drivers.setActive(driver.id, active = driver.status != DriverStatus.ACTIVE)
    }
}
