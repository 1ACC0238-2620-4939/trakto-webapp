package pe.trako.traktoroute.features.vehicles.domain

import java.math.BigDecimal

internal data class Vehicle(
    val id: String,
    val plateNumber: String,
    val capacity: BigDecimal,
    val status: VehicleStatus
) {
    val displayPlate: String
        get() = if (plateNumber.length == 6) {
            "${plateNumber.take(3)}-${plateNumber.takeLast(3)}"
        } else {
            plateNumber
        }
}

internal enum class VehicleStatus {
    ACTIVE,
    INACTIVE,
    MAINTENANCE,
    UNKNOWN
}

internal fun normalizePlate(value: String): String {
    val plate = value.trim().uppercase()
    require(plate.matches(Regex("[A-Z]{3}-?[0-9]{3}"))) {
        "Escribe una placa con tres letras y tres números, como ABC-123."
    }
    return plate.replace("-", "")
}

internal fun parseCapacity(value: String): BigDecimal {
    val capacity = value.trim().replace(',', '.').toBigDecimalOrNull()
    require(capacity != null && capacity.signum() > 0) { "La capacidad debe ser un número mayor que cero." }
    return capacity
}
