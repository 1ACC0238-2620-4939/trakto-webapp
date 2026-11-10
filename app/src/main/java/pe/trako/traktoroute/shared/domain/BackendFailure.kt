package pe.trako.traktoroute.shared.domain

import java.io.IOException

internal sealed class BackendFailure(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class Http(val statusCode: Int, val detail: String?) :
        BackendFailure("HTTP $statusCode")

    class Unreachable(cause: IOException) : BackendFailure("Backend unreachable", cause)

    class InvalidResponse(message: String, cause: Throwable? = null) :
        BackendFailure(message, cause)
}
