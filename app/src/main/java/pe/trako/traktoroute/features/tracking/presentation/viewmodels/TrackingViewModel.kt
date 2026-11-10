package pe.trako.traktoroute.features.tracking.presentation.viewmodels

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.trako.traktoroute.features.tracking.domain.GpsSession
import pe.trako.traktoroute.features.tracking.domain.GpsTracker
import pe.trako.traktoroute.features.tracking.domain.StopReason
import pe.trako.traktoroute.features.tracking.domain.Tracking
import pe.trako.traktoroute.features.tracking.domain.TrackingRepository
import pe.trako.traktoroute.features.tracking.domain.TrackingStatus
import pe.trako.traktoroute.shared.domain.BackendFailure
import pe.trako.traktoroute.shared.presentation.viewmodels.RequestViewModel

internal class TrackingViewModel(
    private val tripId: String,
    private val repository: TrackingRepository,
    private val gps: GpsTracker
) : RequestViewModel() {
    private val mutableTracking = MutableStateFlow<Tracking?>(null)
    val tracking = mutableTracking.asStateFlow()
    val session = gps.session
    private var liveRefresh: Job? = null

    fun refresh() = request {
        liveRefresh?.cancel()
        mutableTracking.value = repository.findByTrip(tripId)
    }

    fun refreshAfterPosition() {
        if (requestState.value.busy) return
        liveRefresh?.cancel()
        liveRefresh = viewModelScope.launch {
            try {
                mutableTracking.value = repository.findByTrip(tripId)
            } catch (_: BackendFailure) {
                // Keep the last position until the next update or an explicit refresh.
            }
        }
    }

    fun belongsToThisTrip(session: GpsSession): Boolean = session.tripId == tripId

    fun isTrackingThisTrip(session: GpsSession): Boolean = belongsToThisTrip(session) && session.isRunning

    fun startGps() {
        val activeTracking = mutableTracking.value?.takeIf { it.status == TrackingStatus.ACTIVE } ?: return
        gps.start(tripId, activeTracking.id)
    }

    fun stopGps() = gps.stop(tripId)

    fun changeReason(stopId: String, reason: StopReason) = request {
        liveRefresh?.cancel()
        val current = requireNotNull(mutableTracking.value) { "Actualiza el seguimiento antes de editar la parada." }
        repository.changeStopReason(current.id, stopId, reason)
        mutableTracking.value = current.copy(
            stops = current.stops.map { if (it.id == stopId) it.copy(reason = reason) else it }
        )
    }
}
