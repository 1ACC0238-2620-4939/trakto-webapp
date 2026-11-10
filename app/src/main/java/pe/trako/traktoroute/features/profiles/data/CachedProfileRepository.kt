package pe.trako.traktoroute.features.profiles.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.trako.traktoroute.features.profiles.data.local.ProfileDao
import pe.trako.traktoroute.features.profiles.data.local.ProfileEntity
import pe.trako.traktoroute.features.profiles.data.remote.ProfileApi
import pe.trako.traktoroute.features.profiles.data.remote.ProfileResponse
import pe.trako.traktoroute.features.profiles.domain.Profile
import pe.trako.traktoroute.features.profiles.domain.ProfileRepository
import pe.trako.traktoroute.shared.data.remote.backendCall

internal class CachedProfileRepository(
    private val api: ProfileApi,
    private val dao: ProfileDao
) : ProfileRepository {
    private val lock = Mutex()
    override val profiles: Flow<List<Profile>> = dao.observe().map { it.map(ProfileEntity::toDomain) }

    override suspend fun refresh() = lock.withLock {
        val profiles = backendCall { api.list() }.map(ProfileResponse::toEntity)
        dao.replace(profiles)
    }
}
