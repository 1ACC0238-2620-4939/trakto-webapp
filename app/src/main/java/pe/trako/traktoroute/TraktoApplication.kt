package pe.trako.traktoroute

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import pe.trako.traktoroute.features.drivers.data.CachedDriverRepository
import pe.trako.traktoroute.features.drivers.data.remote.DriverApi
import pe.trako.traktoroute.features.drivers.domain.DriverRepository
import pe.trako.traktoroute.features.profiles.data.CachedProfileRepository
import pe.trako.traktoroute.features.profiles.data.remote.ProfileApi
import pe.trako.traktoroute.features.profiles.domain.ProfileRepository
import pe.trako.traktoroute.features.tracking.data.RemoteTrackingRepository
import pe.trako.traktoroute.features.tracking.data.location.AndroidGpsTracker
import pe.trako.traktoroute.features.tracking.data.remote.TrackingApi
import pe.trako.traktoroute.features.tracking.domain.TrackingRepository
import pe.trako.traktoroute.features.trips.data.CachedTripRepository
import pe.trako.traktoroute.features.trips.data.remote.TripApi
import pe.trako.traktoroute.features.trips.domain.TripRepository
import pe.trako.traktoroute.features.vehicles.data.CachedVehicleRepository
import pe.trako.traktoroute.features.vehicles.data.remote.VehicleApi
import pe.trako.traktoroute.features.vehicles.domain.VehicleRepository
import pe.trako.traktoroute.shared.data.local.OverviewCacheMigration
import pe.trako.traktoroute.shared.data.local.TraktoDatabase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TraktoApplication : Application() {
    internal val container: AppContainer by lazy { AppContainer(this) }
}

internal class AppContainer(context: Context) {
    private val gson = Gson()
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    private val database = Room.databaseBuilder(context, TraktoDatabase::class.java, "trakto.db")
        .addMigrations(OverviewCacheMigration(gson))
        .build()

    val drivers: DriverRepository = CachedDriverRepository(retrofit.create(DriverApi::class.java), database.drivers())
    val vehicles: VehicleRepository = CachedVehicleRepository(retrofit.create(VehicleApi::class.java), database.vehicles())
    val profiles: ProfileRepository = CachedProfileRepository(retrofit.create(ProfileApi::class.java), database.profiles())
    val trips: TripRepository = CachedTripRepository(retrofit.create(TripApi::class.java), database.trips())
    val tracking: TrackingRepository = RemoteTrackingRepository(retrofit.create(TrackingApi::class.java))
    val gpsTracker = AndroidGpsTracker(context)
}
