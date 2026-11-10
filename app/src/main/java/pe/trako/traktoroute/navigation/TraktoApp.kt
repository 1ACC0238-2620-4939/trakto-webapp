package pe.trako.traktoroute.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import pe.trako.traktoroute.AppContainer
import pe.trako.traktoroute.R
import pe.trako.traktoroute.features.drivers.presentation.view.DriversScreen
import pe.trako.traktoroute.features.drivers.presentation.viewmodels.DriversViewModel
import pe.trako.traktoroute.features.profiles.presentation.view.ProfilesScreen
import pe.trako.traktoroute.features.profiles.presentation.viewmodels.ProfilesViewModel
import pe.trako.traktoroute.features.tracking.presentation.viewmodels.TrackingViewModel
import pe.trako.traktoroute.features.trips.presentation.view.NewTripScreen
import pe.trako.traktoroute.features.trips.presentation.view.TripDetailScreen
import pe.trako.traktoroute.features.trips.presentation.view.TripsScreen
import pe.trako.traktoroute.features.trips.presentation.viewmodels.NewTripViewModel
import pe.trako.traktoroute.features.trips.presentation.viewmodels.TripDetailViewModel
import pe.trako.traktoroute.features.trips.presentation.viewmodels.TripsViewModel
import pe.trako.traktoroute.features.vehicles.presentation.view.VehiclesScreen
import pe.trako.traktoroute.features.vehicles.presentation.viewmodels.VehiclesViewModel

private enum class Tab(val label: String, @get:DrawableRes val icon: Int) {
    TRIPS("Viajes", R.drawable.ic_nav_trips),
    DRIVERS("Conductores", R.drawable.ic_nav_drivers),
    VEHICLES("Camiones", R.drawable.ic_nav_vehicles),
    PROFILES("Perfiles", R.drawable.ic_nav_profiles)
}

private const val NEW_TRIP = "new-trip"
private const val TRIP_DETAIL_PREFIX = "trip/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TraktoApp(container: AppContainer, backendAllowed: Boolean, requestBackendAccess: () -> Unit) {
    var backStack by rememberSaveable { mutableStateOf(listOf(Tab.TRIPS.name)) }
    val canGoBack = backStack.size > 1
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }
    val push: (String) -> Unit = { route ->
        if (backStack.last() != route) backStack = backStack + route
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trakto Route") },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = navigateBack) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!canGoBack) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = backStack.last() == tab.name,
                            onClick = { backStack = listOf(tab.name) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (!backendAllowed) LocalNetworkNotice(requestBackendAccess)

            NavDisplay(
                backStack = backStack,
                onBack = navigateBack,
                modifier = Modifier.weight(1f),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = { route ->
                    NavEntry(route) {
                        when (route) {
                            Tab.TRIPS.name -> TripsScreen(
                                model = viewModel { TripsViewModel(container.trips) },
                                backendAllowed = backendAllowed,
                                newTrip = { push(NEW_TRIP) },
                                openTrip = { push(TRIP_DETAIL_PREFIX + it) }
                            )
                            Tab.DRIVERS.name -> DriversScreen(
                                model = viewModel { DriversViewModel(container.drivers, container.profiles) },
                                backendAllowed = backendAllowed
                            )
                            Tab.VEHICLES.name -> VehiclesScreen(
                                model = viewModel { VehiclesViewModel(container.vehicles) },
                                backendAllowed = backendAllowed
                            )
                            Tab.PROFILES.name -> ProfilesScreen(
                                model = viewModel { ProfilesViewModel(container.profiles) },
                                backendAllowed = backendAllowed
                            )
                            NEW_TRIP -> NewTripScreen(
                                model = viewModel {
                                    NewTripViewModel(container.trips, container.drivers, container.vehicles, container.profiles)
                                },
                                backendAllowed = backendAllowed,
                                manageDrivers = { push(Tab.DRIVERS.name) },
                                manageVehicles = { push(Tab.VEHICLES.name) },
                                openTrip = { backStack = listOf(Tab.TRIPS.name, TRIP_DETAIL_PREFIX + it) }
                            )
                            else -> {
                                val id = route.removePrefix(TRIP_DETAIL_PREFIX)
                                TripDetailScreen(
                                    model = viewModel {
                                        TripDetailViewModel(
                                            id, container.trips, container.drivers, container.vehicles,
                                            container.profiles, container.gpsTracker
                                        )
                                    },
                                    trackingModel = viewModel { TrackingViewModel(id, container.tracking, container.gpsTracker) },
                                    backendAllowed = backendAllowed
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LocalNetworkNotice(requestAccess: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Permite el acceso a la red local para conectar con el backend.")
            TextButton(onClick = requestAccess) { Text("Permitir conexión") }
        }
    }
}
