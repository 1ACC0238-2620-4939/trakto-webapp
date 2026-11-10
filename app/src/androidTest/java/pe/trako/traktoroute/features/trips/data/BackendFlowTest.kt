package pe.trako.traktoroute.features.trips.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import pe.trako.traktoroute.features.drivers.data.CachedDriverRepository
import pe.trako.traktoroute.features.drivers.data.remote.DriverApi
import pe.trako.traktoroute.features.drivers.domain.DriverStatus
import pe.trako.traktoroute.features.tracking.data.RemoteTrackingRepository
import pe.trako.traktoroute.features.tracking.data.remote.TrackingApi
import pe.trako.traktoroute.features.trips.data.remote.TripApi
import pe.trako.traktoroute.features.trips.domain.TripAction
import pe.trako.traktoroute.features.trips.domain.TripDraft
import pe.trako.traktoroute.features.trips.domain.TripStatus
import pe.trako.traktoroute.features.trips.domain.Waypoint
import pe.trako.traktoroute.features.vehicles.data.CachedVehicleRepository
import pe.trako.traktoroute.features.vehicles.data.remote.VehicleApi
import pe.trako.traktoroute.shared.data.local.TraktoDatabase
import pe.trako.traktoroute.shared.domain.Coordinates
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class BackendFlowTest {
    @Test
    fun createsFleetAndUpdatesTripStateUsingBackendContract(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TraktoDatabase::class.java).build()
        val requests = mutableListOf<RecordedRequest>()
        val retrofit = fakeBackend(requests)

        try {
            val drivers = CachedDriverRepository(retrofit.create(DriverApi::class.java), database.drivers())
            val vehicles = CachedVehicleRepository(retrofit.create(VehicleApi::class.java), database.vehicles())
            val trips = CachedTripRepository(retrofit.create(TripApi::class.java), database.trips())
            val tracking = RemoteTrackingRepository(retrofit.create(TrackingApi::class.java))
            val scheduled = Instant.parse("2026-11-10T14:00:00Z")
            val origin = Waypoint("Santa Anita", Coordinates(-12.043, -76.971))
            val destination = Waypoint("Miraflores", Coordinates(-12.121, -77.029))

            drivers.create("p1", "a12345678")
            vehicles.create("ABC-123", BigDecimal("1500.50"))
            val driver = drivers.drivers.first().single()
            val vehicle = vehicles.vehicles.first().single()
            assertEquals(DriverStatus.ACTIVE, driver.status)
            assertEquals("ABC-123", vehicle.displayPlate)

            val tripId = trips.create(TripDraft(driver.id, vehicle.id, origin, destination, scheduled))
            trips.refreshTrip(tripId)
            assertEquals(TripStatus.SCHEDULED, trips.observeTrip(tripId).first()?.status)
            assertEquals(25, trips.observeTrip(tripId).first()?.durationMinutes)

            trips.changeStatus(tripId, TripAction.START)
            assertEquals(TripStatus.IN_PROGRESS, trips.observeTrip(tripId).first()?.status)
            val session = checkNotNull(tracking.findByTrip(tripId))
            tracking.sendPosition(session.id, origin.coordinates, scheduled)
            trips.changeStatus(tripId, TripAction.COMPLETE)
            assertEquals(TripStatus.COMPLETED, trips.observeTrip(tripId).first()?.status)

            assertEquals("A12345678", requests.single { it.path == "/api/v1/drivers" }.body["licenseNumber"].asString)
            assertEquals("ABC123", requests.single { it.path == "/api/v1/vehicles" }.body["plateNumber"].asString)
            val createTrip = requests.single { it.method == "POST" && it.path == "/api/v1/trips" }.body
            assertEquals("Santa Anita", createTrip["originAddress"].asString)
            assertEquals(-77.029, createTrip["destinationLongitude"].asDouble, 0.0)
            assertEquals(scheduled.toString(), createTrip["scheduledAt"].asString)
            assertNotNull(requests.single { it.path.endsWith("/start") }.body["startedAt"])
            assertNotNull(requests.single { it.path.endsWith("/complete") }.body["completedAt"])
            assertEquals(scheduled.toString(), requests.single { it.path.endsWith("/positions") }.body["recordedAt"].asString)
        } finally {
            database.close()
        }
    }

    private fun fakeBackend(requests: MutableList<RecordedRequest>): Retrofit {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            val payload = buffer.readUtf8()
            val body = if (payload.isBlank()) JsonObject() else JsonParser.parseString(payload).asJsonObject
            requests.add(RecordedRequest(request.method, path, body))
            val (status, json) = when ("${request.method} $path") {
                "POST /api/v1/drivers" -> 201 to """{"driverId":"d1","profileId":"p1","licenseNumber":"A12345678","status":"ACTIVE"}"""
                "POST /api/v1/vehicles" -> 201 to """{"vehicleId":"v1","plateNumber":"ABC123","capacity":1500.50,"status":"ACTIVE"}"""
                "POST /api/v1/trips" -> 201 to """{"tripId":"t1"}"""
                "GET /api/v1/trips/t1" -> 200 to """
                    {"tripId":"t1","driverId":"d1","vehicleId":"v1","status":"SCHEDULED",
                     "originAddress":"Santa Anita","originLatitude":-12.043,"originLongitude":-76.971,
                     "destinationAddress":"Miraflores","destinationLatitude":-12.121,"destinationLongitude":-77.029,
                     "scheduledAt":"2026-11-10T14:00:00Z","durationMinutes":25}
                """.trimIndent()
                "GET /api/v1/trackings/trips/t1" -> 200 to """{"trackingId":"g1","status":"ACTIVE","stops":[]}"""
                "PATCH /api/v1/trips/t1/start", "PATCH /api/v1/trips/t1/complete",
                "POST /api/v1/trackings/g1/positions" -> 204 to ""
                else -> 404 to """{"message":"Unexpected request"}"""
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(status).message("Fixture").body(json.toResponseBody()).build()
        }.build()

        return Retrofit.Builder().baseUrl("http://127.0.0.1/")
            .client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }

    private data class RecordedRequest(val method: String, val path: String, val body: JsonObject)
}
