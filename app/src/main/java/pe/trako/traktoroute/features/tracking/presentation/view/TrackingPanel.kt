package pe.trako.traktoroute.features.tracking.presentation.view

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.trako.traktoroute.features.tracking.domain.GpsPhase
import pe.trako.traktoroute.features.tracking.domain.StopReason
import pe.trako.traktoroute.features.tracking.domain.TrackingStatus
import pe.trako.traktoroute.features.tracking.presentation.viewmodels.TrackingViewModel
import pe.trako.traktoroute.shared.presentation.view.components.BadgeTone
import pe.trako.traktoroute.shared.presentation.view.components.ButtonPrimaryComponent
import pe.trako.traktoroute.shared.presentation.view.components.Panel
import pe.trako.traktoroute.shared.presentation.view.components.Picker
import pe.trako.traktoroute.shared.presentation.view.components.RequestFeedback
import pe.trako.traktoroute.shared.presentation.view.components.SecondaryText
import pe.trako.traktoroute.shared.presentation.view.components.SectionTitle
import pe.trako.traktoroute.shared.presentation.view.components.StatusBadge
import pe.trako.traktoroute.shared.presentation.view.components.displayTime

@Composable
internal fun TrackingPanel(
    model: TrackingViewModel,
    tripActive: Boolean,
    backendAllowed: Boolean,
    canShare: Boolean
) {
    val tracking by model.tracking.collectAsStateWithLifecycle()
    val state by model.requestState.collectAsStateWithLifecycle()
    val session by model.session.collectAsStateWithLifecycle()
    val currentTracking = tracking
    val runningHere = model.isTrackingThisTrip(session)
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        permissionDenied = !granted
        if (granted && canShare) model.startGps()
    }

    LaunchedEffect(tripActive, backendAllowed) { if (backendAllowed) model.refresh() }
    LaunchedEffect(runningHere, session.lastSentAt) {
        if (backendAllowed && runningHere && session.lastSentAt != null) model.refreshAfterPosition()
    }

    Panel {
        SectionTitle("Seguimiento GPS")
        RequestFeedback(state)
        if (currentTracking == null) {
            if (!state.busy) SecondaryText("El backend todavía no devuelve un seguimiento para este viaje.")
        } else {
            when (currentTracking.status) {
                TrackingStatus.ACTIVE -> StatusBadge("Seguimiento activo", BadgeTone.POSITIVE)
                TrackingStatus.FINISHED -> StatusBadge("Seguimiento finalizado")
                TrackingStatus.UNKNOWN -> StatusBadge("Estado desconocido", BadgeTone.WARNING)
            }
            val position = currentTracking.lastPosition
            if (position != null) {
                Text("Última posición · ${position.latitude}, ${position.longitude}")
                currentTracking.lastRecordedAt?.let { SecondaryText(displayTime(it)) }
            } else {
                SecondaryText("Todavía no hay posiciones registradas.")
            }

            if (tripActive && currentTracking.status == TrackingStatus.ACTIVE) {
                if (runningHere) {
                    OutlinedButton(onClick = model::stopGps) { Text("Pausar GPS") }
                } else {
                    if (session.isRunning) SecondaryText("Otro viaje está usando el GPS de este dispositivo.")
                    ButtonPrimaryComponent("Compartir ubicación", canShare && !session.isRunning && !state.busy) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionDenied = false
                            model.startGps()
                        } else {
                            permission.launch(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        }
                    }
                }
                if (model.belongsToThisTrip(session)) session.phase.message()?.let { SecondaryText(it) }
                if (permissionDenied) SecondaryText("Permite el acceso a la ubicación precisa para compartir el GPS.")
            }
        }
        OutlinedButton(onClick = model::refresh, enabled = backendAllowed && !state.busy) {
            Text("Actualizar seguimiento")
        }
    }

    if (currentTracking != null) {
        Panel {
            SectionTitle("Paradas")
            if (currentTracking.stops.isEmpty()) SecondaryText("Sin paradas registradas.")
            currentTracking.stops.forEach { stop ->
                HorizontalDivider()
                Text("${displayTime(stop.startedAt)} · ${stop.endedAt?.let(::displayTime) ?: "En curso"}")
                SecondaryText("${stop.coordinates.latitude}, ${stop.coordinates.longitude}")
                Picker("Motivo", StopReason.entries, stop.reason, StopReason::label, backendAllowed && !state.busy) {
                    if (it != stop.reason) model.changeReason(stop.id, it)
                }
            }
        }
    }
}

private fun GpsPhase.message(): String? = when (this) {
    GpsPhase.IDLE -> null
    GpsPhase.STARTING -> "Iniciando GPS…"
    GpsPhase.SEARCHING -> "Buscando señal GPS…"
    GpsPhase.SHARING -> "Última posición enviada correctamente."
    GpsPhase.RETRYING -> "No se pudo enviar la posición. Se intentará con la próxima lectura."
    GpsPhase.UNAVAILABLE -> "No se pudo iniciar el GPS. Revisa el permiso y la ubicación del dispositivo."
    GpsPhase.FINISHED -> "El seguimiento del servidor ya finalizó."
}

private fun StopReason.label(): String = when (this) {
    StopReason.TRAFFIC -> "Tráfico"
    StopReason.REST -> "Descanso"
    StopReason.LOADING -> "Carga"
    StopReason.UNLOADING -> "Descarga"
    StopReason.BREAKDOWN -> "Avería"
    StopReason.UNKNOWN -> "Sin clasificar"
}
