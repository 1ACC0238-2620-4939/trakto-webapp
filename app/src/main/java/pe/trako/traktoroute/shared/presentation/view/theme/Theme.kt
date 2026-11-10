package pe.trako.traktoroute.shared.presentation.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object LightTokens {
    val brand = Color(0xFF007BDF)
    val brandSecondary = Color(0xFF00265F)
    val background = Color(0xFFFAFAFA)
    val backgroundSecondary = Color(0xFFE8F3FF)
    val surface = Color(0xFFFFFFFF)
    val surfaceSecondary = Color(0xFFFAFAFA)
    val surfaceElevated = Color(0xFFFFFFFF)
    val textPrimary = Color(0xFF00265F)
    val textSecondary = Color(0xFF5B6472)
    val textDisabled = Color(0xFF748297)
    val textLink = Color(0xFF0069BD)
    val onPrimary = Color(0xFFFFFFFF)
    val onSecondary = Color(0xFFFFFFFF)
    val onError = Color(0xFFFFFFFF)
    val border = Color(0xFFDCE2E8)
    val borderStrong = Color(0xFF8191A7)
    val divider = Color(0xFFDCE2E8)
    val buttonPrimary = Color(0xFF0073CE)
    val selectedBg = Color(0xFFE8F3FF)
    val selectedText = Color(0xFF0069BD)
    val success = Color(0xFF2E7D32)
    val successSurface = Color(0xFFEDF7EE)
    val warning = Color(0xFFA15C00)
    val warningSurface = Color(0xFFFFF6E8)
    val error = Color(0xFFB3261E)
    val errorSurface = Color(0xFFFDEEEE)
    val info = Color(0xFF0069BD)
    val infoSurface = Color(0xFFE8F3FF)
    val skeletonBase = Color(0xFFE1E6EB)
    val skeletonHighlight = Color(0xFFF0F5FA)
    val mapBackground = Color(0xFFEEF4F8)
    val mapStreet = Color(0xFFDCE7EE)
    val mapStreetHighlighted = Color(0xFFFFFFFF)
    val mapRoute = Color(0xFF007BDF)
}

internal object DarkTokens {
    val brand = Color(0xFF007BDF)
    val brandSecondary = Color(0xFF00265F)
    val background = Color(0xFF101925)
    val backgroundSecondary = Color(0xFF14283E)
    val surface = Color(0xFF1A2635)
    val surfaceSecondary = Color(0xFF202F41)
    val surfaceElevated = Color(0xFF243448)
    val textPrimary = Color(0xFFE7EFFA)
    val textSecondary = Color(0xFFB4C1D2)
    val textDisabled = Color(0xFF8A9AAE)
    val textLink = Color(0xFF70BFFF)
    val onPrimary = Color(0xFFFFFFFF)
    val onSecondary = Color(0xFFFFFFFF)
    val onError = Color(0xFF101925)
    val border = Color(0xFF3A4B61)
    val borderStrong = Color(0xFF7489A5)
    val divider = Color(0xFF304156)
    val buttonPrimary = Color(0xFF0073CE)
    val selectedBg = Color(0xFF142F49)
    val selectedText = Color(0xFF70BFFF)
    val success = Color(0xFF81D38A)
    val successSurface = Color(0xFF173629)
    val warning = Color(0xFFF7C66F)
    val warningSurface = Color(0xFF3B2E1B)
    val error = Color(0xFFFFA39B)
    val errorSurface = Color(0xFF3C242B)
    val info = Color(0xFF70BFFF)
    val infoSurface = Color(0xFF142F49)
    val skeletonBase = Color(0xFF304156)
    val skeletonHighlight = Color(0xFF40556D)
    val mapBackground = Color(0xFF152534)
    val mapStreet = Color(0xFF3B5368)
    val mapStreetHighlighted = Color(0xFF617B91)
    val mapRoute = Color(0xFF53B4FF)
}

private val lightScheme = lightColorScheme(
    primary = LightTokens.buttonPrimary,
    onPrimary = LightTokens.onPrimary,
    secondary = LightTokens.brandSecondary,
    onSecondary = LightTokens.onSecondary,
    primaryContainer = LightTokens.selectedBg,
    onPrimaryContainer = LightTokens.selectedText,
    background = LightTokens.background,
    onBackground = LightTokens.textPrimary,
    surface = LightTokens.surface,
    onSurface = LightTokens.textPrimary,
    surfaceVariant = LightTokens.surfaceSecondary,
    onSurfaceVariant = LightTokens.textSecondary,
    outline = LightTokens.borderStrong,
    outlineVariant = LightTokens.border,
    error = LightTokens.error,
    onError = LightTokens.onError,
    errorContainer = LightTokens.errorSurface,
    onErrorContainer = LightTokens.error
)

private val darkScheme = darkColorScheme(
    primary = DarkTokens.buttonPrimary,
    onPrimary = DarkTokens.onPrimary,
    secondary = DarkTokens.brandSecondary,
    onSecondary = DarkTokens.onSecondary,
    primaryContainer = DarkTokens.selectedBg,
    onPrimaryContainer = DarkTokens.selectedText,
    background = DarkTokens.background,
    onBackground = DarkTokens.textPrimary,
    surface = DarkTokens.surface,
    onSurface = DarkTokens.textPrimary,
    surfaceVariant = DarkTokens.surfaceSecondary,
    onSurfaceVariant = DarkTokens.textSecondary,
    outline = DarkTokens.borderStrong,
    outlineVariant = DarkTokens.border,
    error = DarkTokens.error,
    onError = DarkTokens.onError,
    errorContainer = DarkTokens.errorSurface,
    onErrorContainer = DarkTokens.error
)

@Composable
fun TraktoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme, content = content)
}
