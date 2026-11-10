package pe.trako.traktoroute.features.vehicles.domain

import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

internal interface VehicleRepository {
    val vehicles: Flow<List<Vehicle>>
    suspend fun refresh()
    suspend fun create(plateNumber: String, capacity: BigDecimal)
    suspend fun updatePlate(vehicleId: String, plateNumber: String)
    suspend fun updateCapacity(vehicleId: String, capacity: BigDecimal)
    suspend fun setActive(vehicleId: String, active: Boolean)
}
