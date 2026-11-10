package pe.trako.traktoroute.features.tracking.domain

import pe.trako.traktoroute.shared.domain.Coordinates
import java.time.Instant

internal data class Tracking(
    val id: String,
    val status: TrackingStatus,
    val lastPosition: Coordinates?,
    val lastRecordedAt: Instant?,
    val stops: List<TrackingStop>
)

internal data class TrackingStop(
    val id: String,
    val coordinates: Coordinates,
    val startedAt: Instant,
    val endedAt: Instant?,
    val reason: StopReason
)

internal enum class TrackingStatus { ACTIVE, FINISHED, UNKNOWN }

internal enum class StopReason { TRAFFIC, REST, LOADING, UNLOADING, BREAKDOWN, UNKNOWN }
