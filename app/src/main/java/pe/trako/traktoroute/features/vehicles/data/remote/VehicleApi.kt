package pe.trako.traktoroute.features.vehicles.data.remote

import pe.trako.traktoroute.features.vehicles.data.local.VehicleEntity
import pe.trako.traktoroute.shared.data.remote.requiredField
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigDecimal

internal interface VehicleApi {
    @GET("api/v1/vehicles")
    suspend fun list(): List<VehicleResponse>

    @POST("api/v1/vehicles")
    suspend fun create(@Body body: CreateVehicleRequest): VehicleResponse

    @PATCH("api/v1/vehicles/{id}/plate-number")
    suspend fun updatePlate(@Path("id") id: String, @Body body: PlateRequest): VehicleResponse

    @PATCH("api/v1/vehicles/{id}/capacity")
    suspend fun updateCapacity(@Path("id") id: String, @Body body: CapacityRequest): VehicleResponse

    @PATCH("api/v1/vehicles/{id}/{operation}")
    suspend fun changeStatus(@Path("id") id: String, @Path("operation") operation: String): VehicleResponse
}

internal data class CreateVehicleRequest(val plateNumber: String, val capacity: BigDecimal)
internal data class PlateRequest(val plateNumber: String)
internal data class CapacityRequest(val capacity: BigDecimal)

internal data class VehicleResponse(
    val vehicleId: String?,
    val plateNumber: String?,
    val capacity: BigDecimal?,
    val status: String?
) {
    fun toEntity(): VehicleEntity = VehicleEntity(
        vehicleId = vehicleId.requiredField("vehicleId"),
        plateNumber = plateNumber.requiredField("plateNumber"),
        capacity = capacity.requiredField("capacity"),
        status = status.requiredField("status")
    )
}
