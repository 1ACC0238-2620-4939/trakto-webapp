package pe.trako.traktoroute

import android.content.pm.PackageManager
import android.net.InetAddresses
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import pe.trako.traktoroute.navigation.TraktoApp
import pe.trako.traktoroute.shared.presentation.view.theme.TraktoTheme
import java.net.URI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TraktoApplication).container
        val needsLocalPermission = Build.VERSION.SDK_INT >= 37 && usesLocalNetwork()

        setContent {
            TraktoTheme {
                var permissionRequested by rememberSaveable { mutableStateOf(false) }
                var backendAllowed by remember {
                    mutableStateOf(!needsLocalPermission || checkSelfPermission(LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED)
                }
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    backendAllowed = it
                }
                val requestAccess: () -> Unit = {
                    permissionRequested = true
                    permission.launch(LOCAL_NETWORK)
                }
                LaunchedEffect(Unit) {
                    if (!backendAllowed && !permissionRequested) requestAccess()
                }
                TraktoApp(container, backendAllowed, requestAccess)
            }
        }
    }

    private fun usesLocalNetwork(): Boolean {
        val host = URI(BuildConfig.BACKEND_URL).host ?: return false
        if (host.endsWith(".local", ignoreCase = true)) return true
        return runCatching { InetAddresses.parseNumericAddress(host) }.getOrNull()?.let { address ->
            address.isSiteLocalAddress || address.isLinkLocalAddress
        } ?: false
    }

    private companion object {
        const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
