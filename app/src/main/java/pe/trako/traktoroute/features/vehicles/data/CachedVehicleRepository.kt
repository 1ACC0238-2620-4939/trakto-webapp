package pe.trako.traktoroute.features.vehicles.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.trako.traktoroute.features.vehicles.data.local.VehicleDao
import pe.trako.traktoroute.features.vehicles.data.local.VehicleEntity
import pe.trako.traktoroute.features.vehicles.data.remote.CapacityRequest
import pe.trako.traktoroute.features.vehicles.data.remote.CreateVehicleRequest
import pe.trako.traktoroute.features.vehicles.data.remote.PlateRequest
import pe.trako.traktoroute.features.vehicles.data.remote.VehicleApi
import pe.trako.traktoroute.features.vehicles.data.remote.VehicleResponse
import pe.trako.traktoroute.features.vehicles.domain.Vehicle
import pe.trako.traktoroute.features.vehicles.domain.VehicleRepository
import pe.trako.traktoroute.features.vehicles.domain.normalizePlate
import pe.trako.traktoroute.shared.data.remote.backendCall
import java.math.BigDecimal

internal class CachedVehicleRepository(
    private val api: VehicleApi,
    private val dao: VehicleDao
) : VehicleRepository {
    private val lock = Mutex()
    override val vehicles: Flow<List<Vehicle>> = dao.observe().map { it.map(VehicleEntity::toDomain) }

    override suspend fun refresh() = lock.withLock {
        val vehicles = backendCall { api.list() }.map(VehicleResponse::toEntity)
        dao.replace(vehicles)
    }

    override suspend fun create(plateNumber: String, capacity: BigDecimal) = save {
        require(capacity.signum() > 0) { "La capacidad debe ser mayor que cero." }
        api.create(CreateVehicleRequest(normalizePlate(plateNumber), capacity))
    }

    override suspend fun updatePlate(vehicleId: String, plateNumber: String) = save {
        api.updatePlate(vehicleId, PlateRequest(normalizePlate(plateNumber)))
    }

    override suspend fun updateCapacity(vehicleId: String, capacity: BigDecimal) = save {
        require(capacity.signum() > 0) { "La capacidad debe ser mayor que cero." }
        api.updateCapacity(vehicleId, CapacityRequest(capacity))
    }

    override suspend fun setActive(vehicleId: String, active: Boolean) = save {
        api.changeStatus(vehicleId, if (active) "activate" else "deactivate")
    }

    private suspend fun save(command: suspend () -> VehicleResponse) = lock.withLock {
        dao.upsert(backendCall(command).toEntity())
    }
}
