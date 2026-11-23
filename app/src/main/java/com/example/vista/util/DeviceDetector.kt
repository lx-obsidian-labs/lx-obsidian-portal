package com.example.vista.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Supported device categories in Vista Watch.
 */
enum class DeviceType {
    PHONE,
    TABLET,
    TV,
    DESKTOP
}

/**
 * TV Display Resolution and Layout Configuration for 10-foot viewing distance.
 */
data class TvConfiguration(
    val isTv: Boolean = false,
    val deviceType: DeviceType = DeviceType.PHONE,
    val overscanPaddingHorizontal: Dp = if (isTv) 48.dp else 16.dp,
    val overscanPaddingVertical: Dp = if (isTv) 28.dp else 16.dp,
    val scaleFactor: Float = if (isTv) 1.25f else 1.0f,
    val focusScaleOnHover: Float = if (isTv) 1.08f else 1.0f,
    val cardCornerRadius: Dp = if (isTv) 12.dp else 8.dp,
    val gridColumns: Int = if (isTv) 5 else 2,
)

object DeviceDetector {

    /**
     * Smart multi-factor detection for Smart TVs, Fire TV, Android TV boxes, Tablets, and Phones.
     *
     * Combines system features, UI mode manager, input device capability (D-pad/remote),
     * and screen orientation heuristics.
     */
    fun detectDeviceType(context: Context): DeviceType {
        val packageManager = context.packageManager
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val configuration = context.resources.configuration

        // Factor 1: System Feature Check (Standard Android TV & Fire TV)
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        @Suppress("DEPRECATION")
        val hasTelevisionFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        val isFireTv = packageManager.hasSystemFeature("amazon.hardware.fire_tv")

        if (hasLeanback || hasTelevisionFeature || isFireTv) {
            return DeviceType.TV
        }

        // Factor 2: UiModeManager Current Mode
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return DeviceType.TV
        }

        // Factor 3: Smart TV Box & OTT Box Heuristics
        // Cheap Android TV boxes often run mobile Android ROMs without declaring FEATURE_LEANBACK,
        // but connect to a TV display via HDMI and rely solely on D-pad remote navigation.
        val hasTouchscreen = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        val isNoTouch = (configuration.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH) || !hasTouchscreen
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val hasDpadRemote = isDpadRemoteAvailable()

        if (isNoTouch && isLandscape && hasDpadRemote) {
            return DeviceType.TV
        }

        // Factor 4: Tablet vs Phone check based on screen smallest width (dp)
        val smallestScreenWidthDp = configuration.smallestScreenWidthDp
        if (smallestScreenWidthDp >= 600) {
            return DeviceType.TABLET
        }

        return DeviceType.PHONE
    }

    /**
     * Checks if a D-pad or remote control is available as an input source.
     */
    private fun isDpadRemoteAvailable(): Boolean {
        val deviceIds = InputDevice.getDeviceIds()
        for (id in deviceIds) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            val hasDpad = (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
            val hasKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
            if (hasDpad || hasKeyboard) {
                val isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    device.isExternal
                } else {
                    true
                }
                if (isExternal || !device.name.contains("touch", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Returns the appropriate layout and screen scaling settings based on device type and screen size.
     */
    fun getTvConfiguration(context: Context, type: DeviceType = detectDeviceType(context)): TvConfiguration {
        val isTv = type == DeviceType.TV
        val config = context.resources.configuration

        val screenWidthDp = config.screenWidthDp
        val scale = when {
            isTv && screenWidthDp >= 1920 -> 1.35f
            isTv -> 1.20f
            type == DeviceType.TABLET -> 1.10f
            else -> 1.0f
        }

        val horizontalPadding = if (isTv) 48.dp else 16.dp
        val verticalPadding = if (isTv) 28.dp else 16.dp
        val columns = when (type) {
            DeviceType.TV -> if (screenWidthDp >= 1920) 6 else 5
            DeviceType.TABLET -> 4
            DeviceType.PHONE -> 2
            DeviceType.DESKTOP -> 6
        }

        return TvConfiguration(
            isTv = isTv,
            deviceType = type,
            overscanPaddingHorizontal = horizontalPadding,
            overscanPaddingVertical = verticalPadding,
            scaleFactor = scale,
            focusScaleOnHover = if (isTv) 1.08f else 1.0f,
            cardCornerRadius = if (isTv) 12.dp else 8.dp,
            gridColumns = columns,
        )
    }
}

/** CompositionLocal for retrieving device type across composables */
val LocalDeviceType: ProvidableCompositionLocal<DeviceType> = staticCompositionLocalOf { DeviceType.PHONE }

/** CompositionLocal for retrieving TV configuration & scaling settings across composables */
val LocalTvConfiguration: ProvidableCompositionLocal<TvConfiguration> = staticCompositionLocalOf { TvConfiguration() }

/**
 * Wraps content with device detection and TV layout scaling context.
 */
@Composable
fun ProvideDeviceConfiguration(
    deviceTypeOverride: DeviceType? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val deviceType = deviceTypeOverride ?: DeviceDetector.detectDeviceType(context)
    val tvConfig = DeviceDetector.getTvConfiguration(context, deviceType)

    CompositionLocalProvider(
        LocalDeviceType provides deviceType,
        LocalTvConfiguration provides tvConfig
    ) {
        content()
    }
}
