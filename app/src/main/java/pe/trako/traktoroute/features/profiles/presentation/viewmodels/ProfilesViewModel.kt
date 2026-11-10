package pe.trako.traktoroute.features.profiles.presentation.viewmodels

import pe.trako.traktoroute.features.profiles.domain.ProfileRepository
import pe.trako.traktoroute.shared.presentation.viewmodels.RequestViewModel

internal class ProfilesViewModel(private val repository: ProfileRepository) : RequestViewModel() {
    val profiles = repository.profiles

    fun refresh() = request { repository.refresh() }
}
