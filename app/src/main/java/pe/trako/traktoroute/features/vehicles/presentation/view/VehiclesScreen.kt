package pe.trako.traktoroute.features.vehicles.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.trako.traktoroute.features.vehicles.domain.Vehicle
import pe.trako.traktoroute.features.vehicles.domain.VehicleStatus
import pe.trako.traktoroute.features.vehicles.presentation.viewmodels.VehiclesViewModel
import pe.trako.traktoroute.shared.presentation.view.components.BadgeTone
import pe.trako.traktoroute.shared.presentation.view.components.ButtonPrimaryComponent
import pe.trako.traktoroute.shared.presentation.view.components.EditorDialog
import pe.trako.traktoroute.shared.presentation.view.components.EmptyState
import pe.trako.traktoroute.shared.presentation.view.components.Field
import pe.trako.traktoroute.shared.presentation.view.components.ListPage
import pe.trako.traktoroute.shared.presentation.view.components.Panel
import pe.trako.traktoroute.shared.presentation.view.components.RequestFeedback
import pe.trako.traktoroute.shared.presentation.view.components.SecondaryText
import pe.trako.traktoroute.shared.presentation.view.components.SectionTitle
import pe.trako.traktoroute.shared.presentation.view.components.StatusBadge

private enum class VehicleEditor { CREATE, PLATE, CAPACITY }

@Composable
internal fun VehiclesScreen(model: VehiclesViewModel, backendAllowed: Boolean) {
    val vehicles by model.vehicles.collectAsStateWithLifecycle(initialValue = emptyList())
    val state by model.requestState.collectAsStateWithLifecycle()
    var editor by rememberSaveable { mutableStateOf<VehicleEditor?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val editable = backendAllowed && !state.busy

    LaunchedEffect(backendAllowed) { if (backendAllowed) model.refresh() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            editor = null
            model.clearFeedback()
        }
    }

    fun openEditor(mode: VehicleEditor, vehicle: Vehicle? = null) {
        model.clearFeedback()
        selectedId = vehicle?.id
        editor = mode
    }

    ListPage("Camiones", "Administra placas, capacidades y disponibilidad de la flota.") {
        item { ButtonPrimaryComponent("Nuevo camión", editable) { openEditor(VehicleEditor.CREATE) } }
        item {
            OutlinedButton(onClick = model::refresh, enabled = editable) { Text("Actualizar camiones") }
        }
        if (editor == null) item { RequestFeedback(state) }
        if (vehicles.isEmpty() && !state.busy) {
            item { EmptyState("Sin camiones", "Registra un camión para poder asignarlo a un viaje.") }
        }
        items(vehicles, key = { it.id }) { vehicle ->
            VehicleCard(
                vehicle = vehicle,
                editable = editable,
                editPlate = { openEditor(VehicleEditor.PLATE, vehicle) },
                editCapacity = { openEditor(VehicleEditor.CAPACITY, vehicle) },
                toggle = { model.toggleActive(vehicle) }
            )
        }
    }

    editor?.let { mode ->
        val vehicle = vehicles.find { it.id == selectedId }
        if (mode == VehicleEditor.CREATE || vehicle != null) key(mode, selectedId) {
            var plate by rememberSaveable { mutableStateOf(vehicle?.displayPlate.orEmpty()) }
            var capacity by rememberSaveable { mutableStateOf(vehicle?.capacity?.toPlainString().orEmpty()) }
            val canSave = when (mode) {
                VehicleEditor.CREATE -> plate.isNotBlank() && capacity.isNotBlank()
                VehicleEditor.PLATE -> plate.isNotBlank()
                VehicleEditor.CAPACITY -> capacity.isNotBlank()
            }
            EditorDialog(
                title = when (mode) {
                    VehicleEditor.CREATE -> "Nuevo camión"
                    VehicleEditor.PLATE -> "Editar placa"
                    VehicleEditor.CAPACITY -> "Editar capacidad"
                },
                state = state,
                canSave = backendAllowed && canSave,
                onSave = {
                    when (mode) {
                        VehicleEditor.CREATE -> model.create(plate, capacity)
                        VehicleEditor.PLATE -> if (vehicle != null) model.updatePlate(vehicle.id, plate)
                        VehicleEditor.CAPACITY -> if (vehicle != null) model.updateCapacity(vehicle.id, capacity)
                    }
                },
                onClose = {
                    editor = null
                    model.clearFeedback()
                }
            ) {
                if (mode != VehicleEditor.CAPACITY) {
                    Field(
                        "Placa (ABC-123)",
                        plate,
                        enabled = !state.busy,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii
                        )
                    ) { plate = it }
                }
                if (mode != VehicleEditor.PLATE) {
                    Field(
                        "Capacidad",
                        capacity,
                        enabled = !state.busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    ) { capacity = it }
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    editable: Boolean,
    editPlate: () -> Unit,
    editCapacity: () -> Unit,
    toggle: () -> Unit
) {
    Panel {
        SectionTitle(vehicle.displayPlate)
        SecondaryText("Capacidad ${vehicle.capacity.toPlainString()}")
        when (vehicle.status) {
            VehicleStatus.ACTIVE -> StatusBadge("Activo", BadgeTone.POSITIVE)
            VehicleStatus.INACTIVE -> StatusBadge("Inactivo")
            VehicleStatus.MAINTENANCE -> StatusBadge("Mantenimiento", BadgeTone.WARNING)
            VehicleStatus.UNKNOWN -> StatusBadge("Estado desconocido", BadgeTone.WARNING)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = editPlate, enabled = editable) { Text("Editar placa") }
            TextButton(onClick = editCapacity, enabled = editable) { Text("Editar capacidad") }
        }
        if (vehicle.status != VehicleStatus.UNKNOWN) {
            OutlinedButton(onClick = toggle, enabled = editable) {
                Text(if (vehicle.status == VehicleStatus.ACTIVE) "Desactivar" else "Activar")
            }
        }
    }
}
