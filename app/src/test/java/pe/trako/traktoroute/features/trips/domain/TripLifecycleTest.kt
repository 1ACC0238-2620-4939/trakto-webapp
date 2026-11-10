package pe.trako.traktoroute.features.trips.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.trako.traktoroute.shared.domain.Coordinates
import java.time.Instant

class TripLifecycleTest {
    private val now = Instant.parse("2026-11-10T14:00:00Z")
    private val origin = Waypoint("Santa Anita", Coordinates(-12.043, -76.971))
    private val destination = Waypoint("Miraflores", Coordinates(-12.121, -77.029))
    private val trip = Trip("trip", "driver", "vehicle", origin, destination, TripStatus.SCHEDULED, now)

    @Test
    fun onlyInProgressTripsCanBeCompleted() {
        assertEquals(listOf(TripAction.START, TripAction.CANCEL), trip.status.actions)
        assertThrows(IllegalArgumentException::class.java) { trip.after(TripAction.COMPLETE, now) }

        val started = trip.after(TripAction.START, now)
        assertEquals(TripStatus.IN_PROGRESS, started.status)
        assertEquals(now, started.startedAt)
        assertEquals(listOf(TripAction.COMPLETE, TripAction.CANCEL), started.status.actions)

        val finished = started.after(TripAction.COMPLETE, now.plusSeconds(600))
        assertEquals(TripStatus.COMPLETED, finished.status)
        assertEquals(now.plusSeconds(600), finished.completedAt)
        assertEquals(emptyList<TripAction>(), finished.status.actions)
        assertThrows(IllegalArgumentException::class.java) { finished.after(TripAction.CANCEL, now) }
    }

    @Test
    fun scheduledAndInProgressTripsCanBeCancelled() {
        for (source in listOf(trip, trip.after(TripAction.START, now))) {
            val cancelled = source.after(TripAction.CANCEL, now.plusSeconds(60))
            assertEquals(TripStatus.CANCELLED, cancelled.status)
            assertEquals(now.plusSeconds(60), cancelled.cancelledAt)
            assertEquals(emptyList<TripAction>(), cancelled.status.actions)
            assertThrows(IllegalArgumentException::class.java) { cancelled.after(TripAction.START, now) }
        }
    }

    @Test
    fun rejectsIdenticalAndOutOfRangeCoordinates() {
        assertThrows(IllegalArgumentException::class.java) {
            TripDraft("driver", "vehicle", origin, origin.copy(address = "Otro nombre"), now)
        }
        assertThrows(IllegalArgumentException::class.java) { Coordinates(91.0, 0.0) }
    }
}
