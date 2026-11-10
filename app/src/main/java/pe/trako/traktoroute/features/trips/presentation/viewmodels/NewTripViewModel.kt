package pe.trako.traktoroute.features.trips.presentation.viewmodels

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.trako.traktoroute.features.drivers.domain.Driver
import pe.trako.traktoroute.features.drivers.domain.DriverRepository
import pe.trako.traktoroute.features.drivers.domain.DriverStatus
import pe.trako.traktoroute.features.profiles.domain.ProfileRepository
import pe.trako.traktoroute.features.trips.domain.TripDraft
import pe.trako.traktoroute.features.trips.domain.TripRepository
import pe.trako.traktoroute.features.trips.domain.Waypoint
import pe.trako.traktoroute.features.vehicles.domain.Vehicle
import pe.trako.traktoroute.features.vehicles.domain.VehicleRepository
import pe.trako.traktoroute.features.vehicles.domain.VehicleStatus
import pe.trako.traktoroute.shared.domain.Coordinates
import pe.trako.traktoroute.shared.presentation.viewmodels.RequestViewModel
import pe.trako.traktoroute.shared.presentation.viewmodels.refreshTogether
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

internal data class DriverOption(val driver: Driver, val label: String)
internal data class TripOptions(
    val drivers: List<DriverOption> = emptyList(),
    val vehicles: List<Vehicle> = emptyList()
)

internal data class WaypointInput(val address: String, val latitude: String, val longitude: String) {
    fun toDomain(): Waypoint {
        val lat = requireNotNull(latitude.trim().replace(',', '.').toDoubleOrNull()) { "Escribe una latitud válida." }
        val lon = requireNotNull(longitude.trim().replace(',', '.').toDoubleOrNull()) { "Escribe una longitud válida." }
        return Waypoint(address.trim(), Coordinates(lat, lon))
    }
}

internal data class TripForm(
    val driverId: String,
    val vehicleId: String,
    val origin: WaypointInput,
    val destination: WaypointInput,
    val scheduledAt: String
)

internal class NewTripViewModel(
    private val trips: TripRepository,
    private val drivers: DriverRepository,
    private val vehicles: VehicleRepository,
    private val profiles: ProfileRepository
) : RequestViewModel() {
    val options = combine(drivers.drivers, vehicles.vehicles, profiles.profiles) { allDrivers, allVehicles, allProfiles ->
        val names = allProfiles.associate { it.id to it.fullName }
        TripOptions(
            drivers = allDrivers.filter { it.status == DriverStatus.ACTIVE }.map { driver ->
                val name = names[driver.profileId]
                val label = if (name.isNullOrBlank()) driver.licenseNumber else "$name · ${driver.licenseNumber}"
                DriverOption(driver, label)
            },
            vehicles = allVehicles.filter { it.status == VehicleStatus.ACTIVE }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripOptions())

    private val mutableCreatedTrip = MutableStateFlow<String?>(null)
    val createdTrip = mutableCreatedTrip.asStateFlow()

    fun refresh() = request {
        refreshTogether(drivers::refresh, vehicles::refresh, profiles::refresh).firstOrNull()?.let { throw it }
    }

    fun create(form: TripForm) = request {
        val currentOptions = options.value
        require(currentOptions.drivers.any { it.driver.id == form.driverId }) { "Selecciona un conductor activo." }
        require(currentOptions.vehicles.any { it.id == form.vehicleId }) { "Selecciona un camión activo." }
        val draft = TripDraft(
            driverId = form.driverId,
            vehicleId = form.vehicleId,
            origin = form.origin.toDomain(),
            destination = form.destination.toDomain(),
            scheduledAt = parseSchedule(form.scheduledAt)
        )
        mutableCreatedTrip.value = trips.create(draft)
    }

    fun consumeCreatedTrip() {
        mutableCreatedTrip.value = null
    }
}

private val scheduleFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)

internal fun defaultSchedule(): String = LocalDateTime.now().plusHours(1).format(scheduleFormat)

private fun parseSchedule(value: String): Instant = try {
    LocalDateTime.parse(value.trim(), scheduleFormat).atZone(ZoneId.systemDefault()).toInstant()
} catch (error: DateTimeParseException) {
    throw IllegalArgumentException("Escribe la fecha en formato AAAA-MM-DD HH:mm.", error)
}
