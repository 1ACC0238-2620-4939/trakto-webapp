package pe.trako.traktoroute.features.profiles.domain

import kotlinx.coroutines.flow.Flow

internal interface ProfileRepository {
    val profiles: Flow<List<Profile>>
    suspend fun refresh()
}
