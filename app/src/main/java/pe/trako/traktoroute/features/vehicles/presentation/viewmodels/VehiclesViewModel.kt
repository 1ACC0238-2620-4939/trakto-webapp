package pe.trako.traktoroute.features.vehicles.presentation.viewmodels

import pe.trako.traktoroute.features.vehicles.domain.Vehicle
import pe.trako.traktoroute.features.vehicles.domain.VehicleRepository
import pe.trako.traktoroute.features.vehicles.domain.VehicleStatus
import pe.trako.traktoroute.features.vehicles.domain.parseCapacity
import pe.trako.traktoroute.shared.presentation.viewmodels.RequestViewModel

internal class VehiclesViewModel(private val repository: VehicleRepository) : RequestViewModel() {
    val vehicles = repository.vehicles

    fun refresh() = request { repository.refresh() }

    fun create(plate: String, capacity: String) = request(save = true) {
        repository.create(plate, parseCapacity(capacity))
    }

    fun updatePlate(vehicleId: String, plate: String) = request(save = true) {
        repository.updatePlate(vehicleId, plate)
    }

    fun updateCapacity(vehicleId: String, capacity: String) = request(save = true) {
        repository.updateCapacity(vehicleId, parseCapacity(capacity))
    }

    fun toggleActive(vehicle: Vehicle) = request {
        repository.setActive(vehicle.id, active = vehicle.status != VehicleStatus.ACTIVE)
    }
}
