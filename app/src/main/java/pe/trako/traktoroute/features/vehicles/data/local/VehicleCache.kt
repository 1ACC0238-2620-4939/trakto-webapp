package pe.trako.traktoroute.features.vehicles.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.trako.traktoroute.features.vehicles.domain.Vehicle
import pe.trako.traktoroute.features.vehicles.domain.VehicleStatus
import java.math.BigDecimal

@Entity(tableName = "vehicles")
internal data class VehicleEntity(
    @PrimaryKey val vehicleId: String,
    val plateNumber: String,
    val capacity: BigDecimal,
    val status: String
) {
    fun toDomain(): Vehicle {
        val vehicleStatus = VehicleStatus.entries.firstOrNull { it.name == status } ?: VehicleStatus.UNKNOWN
        return Vehicle(vehicleId, plateNumber, capacity, vehicleStatus)
    }
}

@Dao
internal interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY plateNumber, vehicleId")
    fun observe(): Flow<List<VehicleEntity>>

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(vehicle: VehicleEntity)

    @Upsert
    suspend fun upsertAll(vehicles: List<VehicleEntity>)

    @Transaction
    suspend fun replace(vehicles: List<VehicleEntity>) {
        deleteAll()
        upsertAll(vehicles)
    }
}
