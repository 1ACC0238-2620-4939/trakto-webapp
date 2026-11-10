package pe.trako.traktoroute.features.drivers.domain

internal data class Driver(
    val id: String,
    val profileId: String,
    val licenseNumber: String,
    val status: DriverStatus
)

internal enum class DriverStatus {
    ACTIVE,
    INACTIVE,
    UNKNOWN
}

internal fun normalizeLicense(value: String): String {
    val license = value.trim().uppercase()
    require(license.matches(Regex("[A-Z][0-9]{8}"))) {
        "Escribe una licencia con una letra y ocho números, como A12345678."
    }
    return license
}
