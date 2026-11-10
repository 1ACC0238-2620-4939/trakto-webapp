package pe.trako.traktoroute.features.trips.presentation.view

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.trako.traktoroute.features.trips.presentation.viewmodels.DriverOption
import pe.trako.traktoroute.features.trips.presentation.viewmodels.NewTripViewModel
import pe.trako.traktoroute.features.trips.presentation.viewmodels.TripForm
import pe.trako.traktoroute.features.trips.presentation.viewmodels.WaypointInput
import pe.trako.traktoroute.features.trips.presentation.viewmodels.defaultSchedule
import pe.trako.traktoroute.features.vehicles.domain.Vehicle
import pe.trako.traktoroute.shared.presentation.view.components.ButtonPrimaryComponent
import pe.trako.traktoroute.shared.presentation.view.components.Field
import pe.trako.traktoroute.shared.presentation.view.components.Page
import pe.trako.traktoroute.shared.presentation.view.components.Panel
import pe.trako.traktoroute.shared.presentation.view.components.Picker
import pe.trako.traktoroute.shared.presentation.view.components.RequestFeedback
import pe.trako.traktoroute.shared.presentation.view.components.SecondaryText
import pe.trako.traktoroute.shared.presentation.view.components.SectionTitle

@Composable
internal fun NewTripScreen(
    model: NewTripViewModel,
    backendAllowed: Boolean,
    manageDrivers: () -> Unit,
    manageVehicles: () -> Unit,
    openTrip: (String) -> Unit
) {
    val options by model.options.collectAsStateWithLifecycle()
    val state by model.requestState.collectAsStateWithLifecycle()
    val createdId by model.createdTrip.collectAsStateWithLifecycle()

    var driverId by rememberSaveable { mutableStateOf<String?>(null) }
    var vehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    var originAddress by rememberSaveable { mutableStateOf("") }
    var originLatitude by rememberSaveable { mutableStateOf("") }
    var originLongitude by rememberSaveable { mutableStateOf("") }
    var destinationAddress by rememberSaveable { mutableStateOf("") }
    var destinationLatitude by rememberSaveable { mutableStateOf("") }
    var destinationLongitude by rememberSaveable { mutableStateOf("") }
    var schedule by rememberSaveable { mutableStateOf(defaultSchedule()) }

    val driver = options.drivers.find { it.driver.id == driverId }
    val vehicle = options.vehicles.find { it.id == vehicleId }
    val editable = backendAllowed && !state.busy

    LaunchedEffect(backendAllowed) { if (backendAllowed) model.refresh() }
    LaunchedEffect(createdId) {
        createdId?.let { id ->
            model.consumeCreatedTrip()
            openTrip(id)
        }
    }

    Page("Nuevo viaje", "Elige la flota e ingresa el origen, el destino y la fecha.") {
        Panel {
            SectionTitle("Asignación")
            Picker("Conductor activo", options.drivers, driver, DriverOption::label, editable) {
                driverId = it.driver.id
            }
            if (options.drivers.isEmpty() && !state.busy) {
                SecondaryText("Necesitas al menos un conductor activo.")
                OutlinedButton(onClick = manageDrivers) { Text("Ir a conductores") }
            }
            Picker("Camión activo", options.vehicles, vehicle, Vehicle::displayPlate, editable) {
                vehicleId = it.id
            }
            if (options.vehicles.isEmpty() && !state.busy) {
                SecondaryText("Necesitas al menos un camión activo.")
                OutlinedButton(onClick = manageVehicles) { Text("Ir a camiones") }
            }
            OutlinedButton(onClick = model::refresh, enabled = editable) {
                Text("Actualizar disponibilidad")
            }
        }
        WaypointFields(
            "Origen", originAddress, originLatitude, originLongitude, editable,
            { originAddress = it }, { originLatitude = it }, { originLongitude = it }
        )
        WaypointFields(
            "Destino", destinationAddress, destinationLatitude, destinationLongitude, editable,
            { destinationAddress = it }, { destinationLatitude = it }, { destinationLongitude = it }
        )
        Panel {
            SectionTitle("Programación")
            Field("Fecha local (AAAA-MM-DD HH:mm)", schedule, enabled = editable) { schedule = it }
            SecondaryText("La hora se interpreta según la zona horaria de este dispositivo.")
        }
        RequestFeedback(state)
        ButtonPrimaryComponent("Programar viaje", editable && driver != null && vehicle != null) {
            if (driver != null && vehicle != null) {
                model.create(
                    TripForm(
                        driverId = driver.driver.id,
                        vehicleId = vehicle.id,
                        origin = WaypointInput(originAddress, originLatitude, originLongitude),
                        destination = WaypointInput(destinationAddress, destinationLatitude, destinationLongitude),
                        scheduledAt = schedule
                    )
                )
            }
        }
    }
}

@Composable
private fun WaypointFields(
    title: String,
    address: String,
    latitude: String,
    longitude: String,
    editable: Boolean,
    changeAddress: (String) -> Unit,
    changeLatitude: (String) -> Unit,
    changeLongitude: (String) -> Unit
) {
    val coordinatesKeyboard = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii)
    Panel {
        SectionTitle(title)
        Field("Dirección de ${title.lowercase()}", address, enabled = editable, onChange = changeAddress)
        Field("Latitud de ${title.lowercase()}", latitude, editable, coordinatesKeyboard, changeLatitude)
        Field("Longitud de ${title.lowercase()}", longitude, editable, coordinatesKeyboard, changeLongitude)
    }
}
