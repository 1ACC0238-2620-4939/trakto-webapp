package pe.trako.traktoroute.shared.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import pe.trako.traktoroute.features.drivers.data.local.DriverDao
import pe.trako.traktoroute.features.drivers.data.local.DriverEntity
import pe.trako.traktoroute.features.profiles.data.local.ProfileDao
import pe.trako.traktoroute.features.profiles.data.local.ProfileEntity
import pe.trako.traktoroute.features.trips.data.local.TripDao
import pe.trako.traktoroute.features.trips.data.local.TripEntity
import pe.trako.traktoroute.features.vehicles.data.local.VehicleDao
import pe.trako.traktoroute.features.vehicles.data.local.VehicleEntity
import java.math.BigDecimal
import java.time.Instant

@Database(
    entities = [ProfileEntity::class, DriverEntity::class, VehicleEntity::class, TripEntity::class],
    version = 2
)
@TypeConverters(CacheConverters::class)
internal abstract class TraktoDatabase : RoomDatabase() {
    abstract fun profiles(): ProfileDao
    abstract fun drivers(): DriverDao
    abstract fun vehicles(): VehicleDao
    abstract fun trips(): TripDao
}

internal class CacheConverters {
    @TypeConverter fun fromDecimal(value: BigDecimal?): String? = value?.toString()
    @TypeConverter fun toDecimal(value: String?): BigDecimal? = value?.toBigDecimal()
    @TypeConverter fun fromInstant(value: Instant?): String? = value?.toString()
    @TypeConverter fun toInstant(value: String?): Instant? = value?.let(Instant::parse)
}
