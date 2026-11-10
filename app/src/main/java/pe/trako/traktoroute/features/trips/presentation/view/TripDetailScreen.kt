package pe.trako.traktoroute.features.trips.presentation.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.trako.traktoroute.features.tracking.presentation.view.TrackingPanel
import pe.trako.traktoroute.features.tracking.presentation.viewmodels.TrackingViewModel
import pe.trako.traktoroute.features.trips.domain.Trip
import pe.trako.traktoroute.features.trips.domain.TripAction
import pe.trako.traktoroute.features.trips.domain.TripStatus
import pe.trako.traktoroute.features.trips.presentation.viewmodels.TripDetailViewModel
import pe.trako.traktoroute.shared.presentation.view.components.ButtonPrimaryComponent
import pe.trako.traktoroute.shared.presentation.view.components.EmptyState
import pe.trako.traktoroute.shared.presentation.view.components.Page
import pe.trako.traktoroute.shared.presentation.view.components.Panel
import pe.trako.traktoroute.shared.presentation.view.components.RequestFeedback
import pe.trako.traktoroute.shared.presentation.view.components.SecondaryText
import pe.trako.traktoroute.shared.presentation.view.components.SectionTitle
import pe.trako.traktoroute.shared.presentation.view.components.displayTime

@Composable
internal fun TripDetailScreen(
    model: TripDetailViewModel,
    trackingModel: TrackingViewModel,
    backendAllowed: Boolean
) {
    val detail by model.detail.collectAsStateWithLifecycle()
    val state by model.requestState.collectAsStateWithLifecycle()
    val verified by model.verified.collectAsStateWithLifecycle()
    var confirmation by rememberSaveable { mutableStateOf<TripAction?>(null) }
    val trip = detail.trip
    val canChange = backendAllowed && verified && !state.busy

    LaunchedEffect(backendAllowed) { if (backendAllowed) model.refresh() }

    Page("Detalle del viaje", "Recorrido, asignación y estado del viaje.") {
        OutlinedButton(onClick = model::refresh, enabled = backendAllowed && !state.busy) {
            Text("Actualizar detalle")
        }
        RequestFeedback(state)
        if (trip == null) {
            if (!state.busy) EmptyState("Viaje no disponible", "Actualiza el detalle para consultar el servidor.")
        } else {
            TripSummary(trip)
            Panel {
                SectionTitle("Asignación")
                Text("Conductor · ${detail.driverLabel}")
                Text("Camión · ${detail.vehicleLabel}")
            }

            if (trip.status.actions.isNotEmpty()) {
                if (!verified && !state.busy) SecondaryText("Actualiza el detalle para habilitar las acciones del viaje.")
                trip.status.actions.forEach { action ->
                    if (action == TripAction.CANCEL) {
                        OutlinedButton(onClick = { confirmation = action }, enabled = canChange) {
                            Text(action.label())
                        }
                    } else {
                        ButtonPrimaryComponent(action.label(), canChange) { confirmation = action }
                    }
                }
            }

            if (trip.status != TripStatus.SCHEDULED && trip.status != TripStatus.UNKNOWN) {
                TrackingPanel(
                    model = trackingModel,
                    tripActive = trip.status == TripStatus.IN_PROGRESS,
                    backendAllowed = backendAllowed,
                    canShare = canChange
                )
            }
        }
    }

    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(action.label()) },
            text = { Text(action.confirmationMessage()) },
            confirmButton = {
                TextButton(enabled = canChange, onClick = {
                    confirmation = null
                    model.changeStatus(action)
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun TripSummary(trip: Trip) {
    Panel {
        TripBadge(trip.status)
        SectionTitle(trip.origin.address)
        SecondaryText("${trip.origin.coordinates.latitude}, ${trip.origin.coordinates.longitude}")
        SectionTitle("→ ${trip.destination.address}")
        SecondaryText("${trip.destination.coordinates.latitude}, ${trip.destination.coordinates.longitude}")
        Text("Programado · ${displayTime(trip.scheduledAt)}")
        trip.startedAt?.let { SecondaryText("Inicio · ${displayTime(it)}") }
        trip.completedAt?.let { SecondaryText("Fin · ${displayTime(it)}") }
        trip.cancelledAt?.let { SecondaryText("Cancelación · ${displayTime(it)}") }
        trip.distanceKm?.let { SecondaryText("Distancia · ${it.toPlainString()} km") }
        trip.durationMinutes?.let { SecondaryText("Duración estimada · $it min") }
    }
}

private fun TripAction.label(): String = when (this) {
    TripAction.START -> "Iniciar viaje"
    TripAction.COMPLETE -> "Completar viaje"
    TripAction.CANCEL -> "Cancelar viaje"
}

private fun TripAction.confirmationMessage(): String = when (this) {
    TripAction.START -> "El viaje quedará en curso y el backend abrirá su seguimiento."
    TripAction.COMPLETE -> "El viaje quedará completado y se cerrará su seguimiento."
    TripAction.CANCEL -> "El viaje quedará cancelado. Si está en curso, también se cerrará su seguimiento."
}
