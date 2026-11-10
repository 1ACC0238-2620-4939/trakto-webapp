package pe.trako.traktoroute.features.profiles.presentation.view

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.trako.traktoroute.features.profiles.presentation.viewmodels.ProfilesViewModel
import pe.trako.traktoroute.shared.presentation.view.components.EmptyState
import pe.trako.traktoroute.shared.presentation.view.components.ListPage
import pe.trako.traktoroute.shared.presentation.view.components.Panel
import pe.trako.traktoroute.shared.presentation.view.components.RequestFeedback
import pe.trako.traktoroute.shared.presentation.view.components.SectionTitle

@Composable
internal fun ProfilesScreen(model: ProfilesViewModel, backendAllowed: Boolean) {
    val profiles by model.profiles.collectAsStateWithLifecycle(initialValue = emptyList())
    val state by model.requestState.collectAsStateWithLifecycle()
    LaunchedEffect(backendAllowed) { if (backendAllowed) model.refresh() }

    ListPage("Perfiles", "Personas registradas que se pueden asociar a un conductor.") {
        item {
            OutlinedButton(onClick = model::refresh, enabled = backendAllowed && !state.busy) {
                Text("Actualizar perfiles")
            }
        }
        item { RequestFeedback(state) }
        if (profiles.isEmpty() && !state.busy) {
            item { EmptyState("Sin perfiles disponibles", "Los perfiles se consultan desde el backend.") }
        }
        items(profiles, key = { it.id }) { profile ->
            Panel { SectionTitle(profile.fullName) }
        }
    }
}
