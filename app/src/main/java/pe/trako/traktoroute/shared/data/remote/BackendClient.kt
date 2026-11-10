package pe.trako.traktoroute.shared.data.remote

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import org.json.JSONException
import org.json.JSONObject
import pe.trako.traktoroute.shared.domain.BackendFailure
import pe.trako.traktoroute.shared.domain.Coordinates
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException

internal suspend fun <T> backendCall(call: suspend () -> T): T = try {
    call()
} catch (error: HttpException) {
    throw BackendFailure.Http(error.code(), error.serverMessage())
} catch (error: JsonParseException) {
    throw BackendFailure.InvalidResponse("Invalid JSON", error)
} catch (error: MalformedJsonException) {
    throw BackendFailure.InvalidResponse("Malformed JSON", error)
} catch (error: EOFException) {
    throw BackendFailure.InvalidResponse("Empty response", error)
} catch (error: IOException) {
    throw BackendFailure.Unreachable(error)
}

private fun HttpException.serverMessage(): String? = try {
    val body = response()?.errorBody()?.use { it.string() }
    body?.let { JSONObject(it).optString("message").takeIf(String::isNotBlank) }
} catch (_: IOException) {
    null
} catch (_: JSONException) {
    null
}

internal fun <T : Any> T?.requiredField(name: String): T =
    this ?: throw BackendFailure.InvalidResponse("Missing field: $name")

internal fun String.backendInstant(): Instant = try {
    Instant.parse(this)
} catch (error: DateTimeParseException) {
    throw BackendFailure.InvalidResponse("Invalid timestamp", error)
}

internal fun backendCoordinates(latitude: Double, longitude: Double): Coordinates = try {
    Coordinates(latitude, longitude)
} catch (error: IllegalArgumentException) {
    throw BackendFailure.InvalidResponse("Invalid coordinates", error)
}
