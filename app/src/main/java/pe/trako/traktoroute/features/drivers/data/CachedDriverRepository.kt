package pe.trako.traktoroute.features.drivers.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.trako.traktoroute.features.drivers.data.local.DriverDao
import pe.trako.traktoroute.features.drivers.data.local.DriverEntity
import pe.trako.traktoroute.features.drivers.data.remote.CreateDriverRequest
import pe.trako.traktoroute.features.drivers.data.remote.DriverApi
import pe.trako.traktoroute.features.drivers.data.remote.DriverResponse
import pe.trako.traktoroute.features.drivers.data.remote.LicenseRequest
import pe.trako.traktoroute.features.drivers.domain.Driver
import pe.trako.traktoroute.features.drivers.domain.DriverRepository
import pe.trako.traktoroute.features.drivers.domain.normalizeLicense
import pe.trako.traktoroute.shared.data.remote.backendCall

internal class CachedDriverRepository(
    private val api: DriverApi,
    private val dao: DriverDao
) : DriverRepository {
    private val lock = Mutex()
    override val drivers: Flow<List<Driver>> = dao.observe().map { it.map(DriverEntity::toDomain) }

    override suspend fun refresh() = lock.withLock {
        val drivers = backendCall { api.list() }.map(DriverResponse::toEntity)
        dao.replace(drivers)
    }

    override suspend fun create(profileId: String, licenseNumber: String) = save {
        require(profileId.isNotBlank()) { "Selecciona un perfil existente." }
        api.create(CreateDriverRequest(profileId, normalizeLicense(licenseNumber)))
    }

    override suspend fun updateLicense(driverId: String, licenseNumber: String) = save {
        api.updateLicense(driverId, LicenseRequest(normalizeLicense(licenseNumber)))
    }

    override suspend fun setActive(driverId: String, active: Boolean) = save {
        api.changeStatus(driverId, if (active) "activate" else "deactivate")
    }

    private suspend fun save(command: suspend () -> DriverResponse) = lock.withLock {
        dao.upsert(backendCall(command).toEntity())
    }
}
