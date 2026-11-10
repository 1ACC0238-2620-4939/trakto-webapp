package pe.trako.traktoroute.features.drivers.data.remote

import pe.trako.traktoroute.features.drivers.data.local.DriverEntity
import pe.trako.traktoroute.shared.data.remote.requiredField
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

internal interface DriverApi {
    @GET("api/v1/drivers")
    suspend fun list(): List<DriverResponse>

    @POST("api/v1/drivers")
    suspend fun create(@Body body: CreateDriverRequest): DriverResponse

    @PATCH("api/v1/drivers/{id}/license-number")
    suspend fun updateLicense(@Path("id") id: String, @Body body: LicenseRequest): DriverResponse

    @PATCH("api/v1/drivers/{id}/{operation}")
    suspend fun changeStatus(@Path("id") id: String, @Path("operation") operation: String): DriverResponse
}

internal data class CreateDriverRequest(val profileId: String, val licenseNumber: String)
internal data class LicenseRequest(val licenseNumber: String)

internal data class DriverResponse(
    val driverId: String?,
    val profileId: String?,
    val licenseNumber: String?,
    val status: String?
) {
    fun toEntity(): DriverEntity = DriverEntity(
        driverId = driverId.requiredField("driverId"),
        profileId = profileId.requiredField("profileId"),
        licenseNumber = licenseNumber.requiredField("licenseNumber"),
        status = status.requiredField("status")
    )
}
