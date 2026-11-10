package pe.trako.traktoroute.features.profiles.data.remote

import pe.trako.traktoroute.features.profiles.data.local.ProfileEntity
import pe.trako.traktoroute.shared.data.remote.requiredField
import retrofit2.http.GET

internal interface ProfileApi {
    @GET("api/v1/profiles")
    suspend fun list(): List<ProfileResponse>
}

internal data class ProfileResponse(
    val profileId: String?,
    val givenNames: String?,
    val paternalSurname: String?,
    val maternalSurname: String?
) {
    fun toEntity(): ProfileEntity = ProfileEntity(
        profileId = profileId.requiredField("profileId"),
        givenNames = givenNames.requiredField("givenNames"),
        paternalSurname = paternalSurname.requiredField("paternalSurname"),
        maternalSurname = maternalSurname
    )
}
