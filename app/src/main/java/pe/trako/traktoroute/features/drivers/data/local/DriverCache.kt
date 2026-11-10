package pe.trako.traktoroute.features.drivers.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.trako.traktoroute.features.drivers.domain.Driver
import pe.trako.traktoroute.features.drivers.domain.DriverStatus

@Entity(tableName = "drivers")
internal data class DriverEntity(
    @PrimaryKey val driverId: String,
    val profileId: String,
    val licenseNumber: String,
    val status: String
) {
    fun toDomain(): Driver {
        val driverStatus = DriverStatus.entries.firstOrNull { it.name == status } ?: DriverStatus.UNKNOWN
        return Driver(driverId, profileId, licenseNumber, driverStatus)
    }
}

@Dao
internal interface DriverDao {
    @Query("SELECT * FROM drivers ORDER BY licenseNumber, driverId")
    fun observe(): Flow<List<DriverEntity>>

    @Query("DELETE FROM drivers")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(driver: DriverEntity)

    @Upsert
    suspend fun upsertAll(drivers: List<DriverEntity>)

    @Transaction
    suspend fun replace(drivers: List<DriverEntity>) {
        deleteAll()
        upsertAll(drivers)
    }
}
