package pe.trako.traktoroute.features.drivers.domain

import kotlinx.coroutines.flow.Flow

internal interface DriverRepository {
    val drivers: Flow<List<Driver>>
    suspend fun refresh()
    suspend fun create(profileId: String, licenseNumber: String)
    suspend fun updateLicense(driverId: String, licenseNumber: String)
    suspend fun setActive(driverId: String, active: Boolean)
}
