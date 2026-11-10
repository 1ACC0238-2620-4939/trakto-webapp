package pe.trako.traktoroute.features.profiles.domain

internal data class Profile(
    val id: String,
    val givenNames: String,
    val paternalSurname: String,
    val maternalSurname: String?
) {
    val fullName: String
        get() = listOfNotNull(givenNames, paternalSurname, maternalSurname)
            .filter(String::isNotBlank)
            .joinToString(" ")
}
