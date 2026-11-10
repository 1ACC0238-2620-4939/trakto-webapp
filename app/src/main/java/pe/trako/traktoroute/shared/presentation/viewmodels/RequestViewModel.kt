package pe.trako.traktoroute.shared.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.trako.traktoroute.shared.domain.BackendFailure

internal data class RequestState(
    val busy: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

internal abstract class RequestViewModel : ViewModel() {
    private val mutableRequest = MutableStateFlow(RequestState())
    val requestState: StateFlow<RequestState> = mutableRequest.asStateFlow()

    fun clearFeedback() {
        if (!mutableRequest.value.busy) mutableRequest.value = RequestState()
    }

    protected fun request(save: Boolean = false, block: suspend () -> Unit) {
        if (mutableRequest.value.busy) return
        mutableRequest.value = RequestState(busy = true)
        viewModelScope.launch {
            try {
                block()
                mutableRequest.value = RequestState(saved = save)
            } catch (error: CancellationException) {
                throw error
            } catch (error: BackendFailure) {
                mutableRequest.value = RequestState(error = error.displayMessage())
            } catch (error: IllegalArgumentException) {
                mutableRequest.value = RequestState(error = error.message ?: "Revisa los datos ingresados.")
            } catch (error: Exception) {
                Log.e("TraktoRoute", "Request failed", error)
                mutableRequest.value = RequestState(error = "No se pudo completar la operación.")
            }
        }
    }
}

internal suspend fun refreshTogether(vararg sources: suspend () -> Unit): List<BackendFailure> = coroutineScope {
    sources.map { refresh ->
        async {
            try {
                refresh()
                null
            } catch (error: BackendFailure) {
                error
            }
        }
    }.awaitAll().filterNotNull()
}

private fun BackendFailure.displayMessage(): String = when (this) {
    is BackendFailure.Unreachable -> "Sin conexión con el backend. Comprueba que esté escuchando en el puerto 8080."
    is BackendFailure.InvalidResponse -> "El backend devolvió datos incompletos o inesperados."
    is BackendFailure.Http -> detail ?: "El backend devolvió un error HTTP $statusCode."
}
