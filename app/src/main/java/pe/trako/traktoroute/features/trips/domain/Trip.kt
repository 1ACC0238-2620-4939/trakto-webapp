package pe.trako.traktoroute.features.trips.domain

import pe.trako.traktoroute.shared.domain.Coordinates
import java.math.BigDecimal
import java.time.Instant

internal data class Trip(
    val id: String,
    val driverId: String,
    val vehicleId: String,
    val origin: Waypoint,
    val destination: Waypoint,
    val status: TripStatus,
    val scheduledAt: Instant,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val cancelledAt: Instant? = null,
    val distanceKm: BigDecimal? = null,
    val durationMinutes: Int? = null
) {
    fun after(action: TripAction, at: Instant): Trip {
        require(action in status.actions) { "Esta acción ya no está disponible para el viaje." }
        return when (action) {
            TripAction.START -> copy(status = TripStatus.IN_PROGRESS, startedAt = at)
            TripAction.COMPLETE -> copy(status = TripStatus.COMPLETED, completedAt = at)
            TripAction.CANCEL -> copy(status = TripStatus.CANCELLED, cancelledAt = at)
        }
    }
}

internal enum class TripStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    UNKNOWN;

    val actions: List<TripAction>
        get() = when (this) {
            SCHEDULED -> listOf(TripAction.START, TripAction.CANCEL)
            IN_PROGRESS -> listOf(TripAction.COMPLETE, TripAction.CANCEL)
            COMPLETED, CANCELLED, UNKNOWN -> emptyList()
        }
}

internal enum class TripAction { START, COMPLETE, CANCEL }

internal data class Waypoint(val address: String, val coordinates: Coordinates) {
    init {
        require(address.isNotBlank()) { "Escribe la dirección." }
    }
}

internal data class TripDraft(
    val driverId: String,
    val vehicleId: String,
    val origin: Waypoint,
    val destination: Waypoint,
    val scheduledAt: Instant
) {
    init {
        require(driverId.isNotBlank()) { "Selecciona un conductor activo." }
        require(vehicleId.isNotBlank()) { "Selecciona un camión activo." }
        val start = origin.coordinates
        val end = destination.coordinates
        require(start.latitude != end.latitude || start.longitude != end.longitude) {
            "El origen y el destino deben tener coordenadas diferentes."
        }
    }
}
