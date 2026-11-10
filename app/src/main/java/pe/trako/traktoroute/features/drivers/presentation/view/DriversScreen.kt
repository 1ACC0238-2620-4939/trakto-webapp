package pe.trako.traktoroute.features.drivers.presentation.view

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.trako.traktoroute.features.drivers.domain.Driver
import pe.trako.traktoroute.features.drivers.domain.DriverStatus
import pe.trako.traktoroute.features.drivers.presentation.viewmodels.DriversViewModel
import pe.trako.traktoroute.features.profiles.domain.Profile
import pe.trako.traktoroute.shared.presentation.view.components.BadgeTone
import pe.trako.traktoroute.shared.presentation.view.components.ButtonPrimaryComponent
import pe.trako.traktoroute.shared.presentation.view.components.EditorDialog
import pe.trako.traktoroute.shared.presentation.view.components.EmptyState
import pe.trako.traktoroute.shared.presentation.view.components.Field
import pe.trako.traktoroute.shared.presentation.view.components.ListPage
import pe.trako.traktoroute.shared.presentation.view.components.Panel
import pe.trako.traktoroute.shared.presentation.view.components.Picker
import pe.trako.traktoroute.shared.presentation.view.components.RequestFeedback
import pe.trako.traktoroute.shared.presentation.view.components.SecondaryText
import pe.trako.traktoroute.shared.presentation.view.components.SectionTitle
import pe.trako.traktoroute.shared.presentation.view.components.StatusBadge

private const val NEW_DRIVER = "new-driver"

@Composable
internal fun DriversScreen(model: DriversViewModel, backendAllowed: Boolean) {
    val directory by model.directory.collectAsStateWithLifecycle()
    val state by model.requestState.collectAsStateWithLifecycle()
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    val editable = backendAllowed && !state.busy

    LaunchedEffect(backendAllowed) { if (backendAllowed) model.refresh() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            editor = null
            model.clearFeedback()
        }
    }

    ListPage("Conductores", "Registra licencias y elige quién está disponible para viajar.") {
        item {
            ButtonPrimaryComponent("Nuevo conductor", editable) {
                model.clearFeedback()
                editor = NEW_DRIVER
            }
        }
        item {
            OutlinedButton(onClick = model::refresh, enabled = editable) { Text("Actualizar conductores") }
        }
        if (editor == null) item { RequestFeedback(state) }
        if (directory.drivers.isEmpty() && !state.busy) {
            item { EmptyState("Sin conductores", "Crea un conductor a partir de un perfil existente.") }
        }
        items(directory.drivers, key = { it.id }) { driver ->
            DriverCard(
                driver = driver,
                name = directory.profiles[driver.profileId]?.fullName,
                editable = editable,
                editLicense = {
                    model.clearFeedback()
                    editor = driver.id
                },
                toggle = { model.toggleActive(driver) }
            )
        }
    }

    editor?.let { target ->
        val driver = directory.drivers.find { it.id == target }
        if (target == NEW_DRIVER || driver != null) key(target) {
            var license by rememberSaveable { mutableStateOf(driver?.licenseNumber.orEmpty()) }
            var profileId by rememberSaveable { mutableStateOf<String?>(null) }
            val profile = directory.availableProfiles.find { it.id == profileId }
            EditorDialog(
                title = if (driver == null) "Nuevo conductor" else "Editar licencia",
                state = state,
                canSave = backendAllowed && license.isNotBlank() && (driver != null || profile != null),
                onSave = {
                    if (driver != null) model.updateLicense(driver.id, license)
                    else if (profile != null) model.create(profile.id, license)
                },
                onClose = {
                    editor = null
                    model.clearFeedback()
                }
            ) {
                if (driver == null) {
                    Picker("Selecciona un perfil existente", directory.availableProfiles, profile, Profile::fullName, !state.busy) {
                        profileId = it.id
                    }
                    if (directory.availableProfiles.isEmpty()) {
                        SecondaryText("No hay perfiles disponibles sin conductor. El perfil debe existir en el backend.")
                    }
                }
                Field(
                    "Licencia (A12345678)",
                    license,
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii
                    )
                ) { license = it }
            }
        }
    }
}

@Composable
private fun DriverCard(
    driver: Driver,
    name: String?,
    editable: Boolean,
    editLicense: () -> Unit,
    toggle: () -> Unit
) {
    Panel {
        SectionTitle(name ?: driver.licenseNumber)
        if (name != null) SecondaryText("Licencia ${driver.licenseNumber}")
        when (driver.status) {
            DriverStatus.ACTIVE -> StatusBadge("Activo", BadgeTone.POSITIVE)
            DriverStatus.INACTIVE -> StatusBadge("Inactivo")
            DriverStatus.UNKNOWN -> StatusBadge("Estado desconocido", BadgeTone.WARNING)
        }
        TextButton(onClick = editLicense, enabled = editable) { Text("Editar licencia") }
        if (driver.status != DriverStatus.UNKNOWN) {
            OutlinedButton(onClick = toggle, enabled = editable) {
                Text(if (driver.status == DriverStatus.ACTIVE) "Desactivar" else "Activar")
            }
        }
    }
}
