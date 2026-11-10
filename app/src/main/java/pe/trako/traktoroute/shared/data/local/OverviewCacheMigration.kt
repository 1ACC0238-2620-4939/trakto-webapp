package pe.trako.traktoroute.shared.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import pe.trako.traktoroute.features.drivers.data.remote.DriverResponse
import pe.trako.traktoroute.features.profiles.data.remote.ProfileResponse
import pe.trako.traktoroute.features.trips.data.remote.TripResponse
import pe.trako.traktoroute.features.vehicles.data.remote.VehicleResponse
import pe.trako.traktoroute.shared.domain.BackendFailure

internal class OverviewCacheMigration(private val gson: Gson) : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE profiles (
                profileId TEXT NOT NULL PRIMARY KEY,
                givenNames TEXT NOT NULL,
                paternalSurname TEXT NOT NULL,
                maternalSurname TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE drivers (
                driverId TEXT NOT NULL PRIMARY KEY,
                profileId TEXT NOT NULL,
                licenseNumber TEXT NOT NULL,
                status TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE vehicles (
                vehicleId TEXT NOT NULL PRIMARY KEY,
                plateNumber TEXT NOT NULL,
                capacity TEXT NOT NULL,
                status TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE trips (
                tripId TEXT NOT NULL PRIMARY KEY,
                driverId TEXT NOT NULL,
                vehicleId TEXT NOT NULL,
                originAddress TEXT NOT NULL,
                originLatitude REAL NOT NULL,
                originLongitude REAL NOT NULL,
                destinationAddress TEXT NOT NULL,
                destinationLatitude REAL NOT NULL,
                destinationLongitude REAL NOT NULL,
                status TEXT NOT NULL,
                scheduledAt TEXT NOT NULL,
                startedAt TEXT,
                completedAt TEXT,
                cancelledAt TEXT,
                distanceKm TEXT,
                durationMinutes INTEGER
            )
            """.trimIndent()
        )

        val snapshot = db.query("SELECT json FROM OverviewCache WHERE id = 0").use { cursor ->
            if (cursor.moveToFirst()) parseSnapshot(cursor.getString(0)) else null
        }
        if (snapshot != null) migrateRows(db, snapshot)
        db.execSQL("DROP TABLE OverviewCache")
    }

    private fun migrateRows(db: SupportSQLiteDatabase, snapshot: JsonObject) {
        restore<ProfileResponse>(snapshot, "profiles") { response ->
            val profile = response.toEntity()
            db.execSQL(
                "INSERT OR REPLACE INTO profiles VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(profile.profileId, profile.givenNames, profile.paternalSurname, profile.maternalSurname)
            )
        }
        restore<DriverResponse>(snapshot, "drivers") { response ->
            val driver = response.toEntity()
            db.execSQL(
                "INSERT OR REPLACE INTO drivers VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(driver.driverId, driver.profileId, driver.licenseNumber, driver.status)
            )
        }
        restore<VehicleResponse>(snapshot, "vehicles") { response ->
            val vehicle = response.toEntity()
            db.execSQL(
                "INSERT OR REPLACE INTO vehicles VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(vehicle.vehicleId, vehicle.plateNumber, vehicle.capacity.toString(), vehicle.status)
            )
        }
        restore<TripResponse>(snapshot, "trips") { response ->
            val trip = response.toEntity()
            db.execSQL(
                "INSERT OR REPLACE INTO trips VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    trip.tripId, trip.driverId, trip.vehicleId,
                    trip.originAddress, trip.originLatitude, trip.originLongitude,
                    trip.destinationAddress, trip.destinationLatitude, trip.destinationLongitude,
                    trip.status, trip.scheduledAt.toString(), trip.startedAt?.toString(),
                    trip.completedAt?.toString(), trip.cancelledAt?.toString(),
                    trip.distanceKm?.toString(), trip.durationMinutes
                )
            )
        }
    }

    private fun parseSnapshot(json: String): JsonObject? = try {
        JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
    } catch (_: JsonParseException) {
        null
    }

    private inline fun <reified T : Any> restore(
        snapshot: JsonObject,
        collection: String,
        insert: (T) -> Unit
    ) {
        val rows = snapshot.get(collection)?.takeIf { it.isJsonArray }?.asJsonArray ?: return
        for (row in rows) {
            try {
                gson.fromJson(row, T::class.java)?.let(insert)
            } catch (_: JsonParseException) {
                // An unreadable cached item can be fetched again.
            } catch (_: BackendFailure.InvalidResponse) {
                // Keep the other items when an old snapshot has an incomplete row.
            }
        }
    }
}
