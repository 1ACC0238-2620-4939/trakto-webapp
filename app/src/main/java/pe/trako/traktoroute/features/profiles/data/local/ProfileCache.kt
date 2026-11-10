package pe.trako.traktoroute.features.profiles.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pe.trako.traktoroute.features.profiles.domain.Profile

@Entity(tableName = "profiles")
internal data class ProfileEntity(
    @PrimaryKey val profileId: String,
    val givenNames: String,
    val paternalSurname: String,
    val maternalSurname: String?
) {
    fun toDomain(): Profile = Profile(profileId, givenNames, paternalSurname, maternalSurname)
}

@Dao
internal interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY givenNames, paternalSurname, profileId")
    fun observe(): Flow<List<ProfileEntity>>

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertAll(profiles: List<ProfileEntity>)

    @Transaction
    suspend fun replace(profiles: List<ProfileEntity>) {
        deleteAll()
        upsertAll(profiles)
    }
}
