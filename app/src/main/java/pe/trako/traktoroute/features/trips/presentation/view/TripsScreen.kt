package pe.trako.traktoroute.features.trips.presentation.view

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.trako.traktoroute.features.trips.domain.TripStatus
import pe.trako.traktoroute.features.trips.presentation.viewmodels.TripsViewModel
import pe.trako.traktoroute.shared.presentation.view.components.BadgeTone
import pe.trako.traktoroute.shared.presentation.view.components.ButtonPrimaryComponent
import pe.trako.traktoroute.shared.presentation.view.components.EmptyState
import pe.trako.traktoroute.shared.presentation.view.components.ListPage
import pe.trako.traktoroute.shared.presentation.view.components.Panel
import pe.trako.traktoroute.shared.presentation.view.components.Picker
import pe.trako.traktoroute.shared.presentation.view.components.RequestFeedback
import pe.trako.traktoroute.shared.presentation.view.components.SecondaryText
import pe.trako.traktoroute.shared.presentation.view.components.SectionTitle
import pe.trako.traktoroute.shared.presentation.view.components.StatusBadge
import pe.trako.traktoroute.shared.presentation.view.components.displayTime

private data class TripFilter(val status: TripStatus?, val label: String)

private val filters = listOf(TripFilter(null, "Todos los estados")) +
    TripStatus.entries.filterNot { it == TripStatus.UNKNOWN }.map { TripFilter(it, it.label()) }

@Composable
internal fun TripsScreen(
    model: TripsViewModel,
    backendAllowed: Boolean,
    newTrip: () -> Unit,
    openTrip: (String) -> Unit
) {
    val trips by model.trips.collectAsStateWithLifecycle(initialValue = emptyList())
    val state by model.requestState.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<TripStatus?>(null) }
    val visibleTrips = trips.filter { filter == null || it.status == filter }

    LaunchedEffect(backendAllowed) { if (backendAllowed) model.refresh() }

    ListPage("Viajes", "Programa los recorridos y consulta qué viajes están en curso.") {
        item { ButtonPrimaryComponent("Nuevo viaje", backendAllowed, onClick = newTrip) }
        item {
            OutlinedButton(onClick = model::refresh, enabled = backendAllowed && !state.busy) {
                Text("Actualizar viajes")
            }
        }
        item {
            Picker("Estado del viaje", filters, filters.find { it.status == filter }, TripFilter::label) {
                filter = it.status
            }
        }
        if (state.busy || state.error != null) item { RequestFeedback(state) }
        if (visibleTrips.isEmpty() && !state.busy) {
            item {
                if (filter == null) EmptyState("Sin viajes", "Asigna un conductor y un camión activos para programar un viaje.")
                else EmptyState("Sin viajes en este estado", "Cambia el filtro para ver otros viajes.")
            }
        }
        items(visibleTrips, key = { it.id }) { trip ->
            Panel {
                TripBadge(trip.status)
                SectionTitle(trip.origin.address)
                SectionTitle("→ ${trip.destination.address}")
                SecondaryText("Programado para ${displayTime(trip.scheduledAt)}")
                OutlinedButton(onClick = { openTrip(trip.id) }) { Text("Ver viaje") }
            }
        }
    }
}

@Composable
internal fun TripBadge(status: TripStatus) {
    val tone = when (status) {
        TripStatus.SCHEDULED -> BadgeTone.INFO
        TripStatus.IN_PROGRESS -> BadgeTone.POSITIVE
        TripStatus.COMPLETED -> BadgeTone.NEUTRAL
        TripStatus.CANCELLED -> BadgeTone.ERROR
        TripStatus.UNKNOWN -> BadgeTone.WARNING
    }
    StatusBadge(status.label(), tone)
}

private fun TripStatus.label(): String = when (this) {
    TripStatus.SCHEDULED -> "Programado"
    TripStatus.IN_PROGRESS -> "En curso · Activo"
    TripStatus.COMPLETED -> "Completado"
    TripStatus.CANCELLED -> "Cancelado"
    TripStatus.UNKNOWN -> "Estado desconocido"
}
