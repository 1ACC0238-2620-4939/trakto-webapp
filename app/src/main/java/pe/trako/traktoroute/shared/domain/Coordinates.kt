package pe.trako.traktoroute.shared.domain

internal data class Coordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "La latitud debe estar entre -90 y 90." }
        require(longitude in -180.0..180.0) { "La longitud debe estar entre -180 y 180." }
    }
}
