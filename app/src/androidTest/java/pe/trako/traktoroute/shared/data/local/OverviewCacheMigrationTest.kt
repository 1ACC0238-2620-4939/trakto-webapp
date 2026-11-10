package pe.trako.traktoroute.shared.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import pe.trako.traktoroute.features.trips.domain.TripStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OverviewCacheMigrationTest {
    @Test
    fun migratesVersionOneOverviewIntoFeatureTables(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()

        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL("CREATE TABLE OverviewCache (id INTEGER NOT NULL PRIMARY KEY, json TEXT NOT NULL)")
                old.execSQL("INSERT INTO OverviewCache (id, json) VALUES (0, ?)", arrayOf(legacyOverview))
                old.version = 1
            }
            val database = Room.databaseBuilder(context, TraktoDatabase::class.java, databaseName)
                .addMigrations(OverviewCacheMigration(Gson()))
                .build()
            try {
                assertEquals("Ana Pérez", database.profiles().observe().first().single().toDomain().fullName)
                assertEquals("A12345678", database.drivers().observe().first().single().licenseNumber)
                assertEquals(BigDecimal("1500.50"), database.vehicles().observe().first().single().capacity)
                val trip = database.trips().observeTrip("t1").first()?.toDomain()
                assertEquals(TripStatus.IN_PROGRESS, trip?.status)
                assertEquals(Instant.parse("2026-11-10T14:00:00Z"), trip?.scheduledAt)
                assertEquals(25, trip?.durationMinutes)
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private val legacyOverview = """
        {
          "profiles": [{"profileId": "p1", "givenNames": "Ana", "paternalSurname": "Pérez"}],
          "drivers": [
            {"driverId": "d1", "profileId": "p1", "licenseNumber": "A12345678", "status": "ACTIVE"},
            {"driverId": "incomplete"}
          ],
          "vehicles": [{"vehicleId": "v1", "plateNumber": "ABC-123", "capacity": 1500.50, "status": "ACTIVE"}],
          "trips": [{
            "tripId": "t1", "driverId": "d1", "vehicleId": "v1", "status": "IN_PROGRESS",
            "originAddress": "Santa Anita", "originLatitude": -12.043, "originLongitude": -76.971,
            "destinationAddress": "Miraflores", "destinationLatitude": -12.121, "destinationLongitude": -77.029,
            "scheduledAt": "2026-11-10T14:00:00Z", "distanceKm": 12, "durationMinutes": 25.0
          }]
        }
    """.trimIndent()
}
