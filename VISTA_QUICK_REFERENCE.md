# 📱 Vista Watch — Quick Reference & Copy-Paste Snippets

Maintained by **LX Obsidian Labs** for **Vista Watch**. Some examples below describe earlier implementations; the source files are authoritative. Android 1.0.12 uses a shared phone/TV APK, the original VISTA wordmark, and official support at `support@lxobsidianportal.co.za`.

Current additions include `support/VistaContacts.kt`, reviewable support reports, account error dialogs, `artwork/VistaArtworkRegistry.kt`, `HomeCatalogLoader.kt`, `EmbedNavigationPolicy.kt`, and `TvPlaybackOptions.kt`. Use the existing `ProvideDeviceConfiguration(deviceTypeOverride = DeviceType.TV)` for the dedicated TV entry point. `screenWidthDp` is a logical width, not a physical 1080p/4K resolution measurement.

---

## 🚀 1. Build & Release Commands

```bash
# Clean & assemble Debug APK (on Windows use .\gradlew.bat)
./gradlew :app:clean :app:assembleDebug --stacktrace

# Run all unit tests; read the actual count from the generated test reports
./gradlew app:testDebugUnitTest

# Assemble Release APK
./gradlew app:assembleRelease
```

---

## 🛡️ 2. ProGuard Keep Rules for R8 Minification (`app/proguard-rules.pro`)

Review these examples against the existing `app/proguard-rules.pro`; they do not establish that R8 is safe for this release. Release minification is currently disabled after a previously reproduced empty-catalog regression. Any minified candidate needs separate metadata, artwork, Firebase and playback verification.

```proguard
# Preserve Gson DTO models and Retrofit interfaces
-keepclassmembers class com.example.vista.data.remote.** { *; }
-keep class com.example.vista.data.remote.** { *; }
-keepclassmembers class com.example.vista.domain.model.** { *; }
-keep class com.example.vista.domain.model.** { *; }

# Gson reflective serialization rules
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class * extends com.google.gson.TypeAdapter { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Media3 ExoPlayer rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
```

Keep the shipping configuration in `app/build.gradle.kts` until a minified candidate has passed those checks:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        isShrinkResources = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

---

## 📺 3. Smart TV Detection & 10-Foot Layout Provider (`util/DeviceDetector.kt`)

Reference only: the project already contains this utility with a TV entry-point override. Do not overwrite the current implementation with this older abbreviated example:

```kotlin
// Location: com.example.vista.util.DeviceDetector

enum class DeviceType { PHONE, TABLET, TV, DESKTOP }

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
    fun detectDeviceType(context: Context): DeviceType {
        val pm = context.packageManager
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val config = context.resources.configuration

        val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        @Suppress("DEPRECATION")
        val hasTvFeature = pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        val isFireTv = pm.hasSystemFeature("amazon.hardware.fire_tv")

        if (hasLeanback || hasTvFeature || isFireTv) return DeviceType.TV
        if (uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return DeviceType.TV

        val isNoTouch = (config.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH) || !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isNoTouch && isLandscape && hasDpadRemote()) return DeviceType.TV

        if (config.smallestScreenWidthDp >= 600) return DeviceType.TABLET
        return DeviceType.PHONE
    }

    private fun hasDpadRemote(): Boolean {
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            val sources = device.sources
            val hasDpad = (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
            val hasKb = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
            if (hasDpad || hasKb) {
                val isExt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) device.isExternal else true
                if (isExt || !device.name.contains("touch", ignoreCase = true)) return true
            }
        }
        return false
    }

    fun getTvConfiguration(context: Context): TvConfiguration {
        val type = detectDeviceType(context)
        val isTv = type == DeviceType.TV
        val config = context.resources.configuration
        val width = config.screenWidthDp

        val scale = when {
            isTv && width >= 1920 -> 1.35f
            isTv -> 1.20f
            type == DeviceType.TABLET -> 1.10f
            else -> 1.0f
        }

        return TvConfiguration(
            isTv = isTv,
            deviceType = type,
            overscanPaddingHorizontal = if (isTv) 48.dp else 16.dp,
            overscanPaddingVertical = if (isTv) 28.dp else 16.dp,
            scaleFactor = scale,
            focusScaleOnHover = if (isTv) 1.08f else 1.0f,
            cardCornerRadius = if (isTv) 12.dp else 8.dp,
            gridColumns = when (type) {
                DeviceType.TV -> if (width >= 1920) 6 else 5
                DeviceType.TABLET -> 4
                DeviceType.PHONE -> 2
                DeviceType.DESKTOP -> 6
            }
        )
    }
}

val LocalDeviceType = staticCompositionLocalOf { DeviceType.PHONE }
val LocalTvConfiguration = staticCompositionLocalOf { TvConfiguration() }

@Composable
fun ProvideDeviceConfiguration(content: @Composable () -> Unit) {
    val context = LocalContext.current
    CompositionLocalProvider(
        LocalDeviceType provides DeviceDetector.detectDeviceType(context),
        LocalTvConfiguration provides DeviceDetector.getTvConfiguration(context),
        content = content
    )
}
```

---

## 🎥 4. Media3 Stream Error Recovery Overlay Snippet (`ui/player/PlayerScreen.kt`)

```kotlin
if (uiState.error != null) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp)
            .background(CardSurface, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Warning, contentDescription = null, tint = Accent, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = ViewUtils.formatError(uiState.error),
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (uiState.canRepairManually) Button(
                onClick = { viewModel.clearError(); viewModel.repairPlaybackAndRetry() },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Repair Playback", color = PrimaryText, fontWeight = FontWeight.Bold)
            }
            if (uiState.streamSources.size > 1) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearError()
                        val alt = uiState.streamSources.firstOrNull { it != uiState.selectedSource }
                        if (alt != null) viewModel.onSourceChange(alt)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Switch Server", color = PrimaryText) }
            } else if (uiState.streamSources.isEmpty()) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearError()
                        navController.navigate(Screen.Search.createRoute(uiState.title))
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Find Alternatives", color = PrimaryText) }
            }
            TextButton(onClick = { viewModel.clearError(); navController.popBackStack() }) {
                Text("Go Back", color = MutedText)
            }
        }
    }
}
```

---

## 🔔 5. Notification Channel Setup (`VistaMessagingService.kt`)

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val updatesChannel = NotificationChannel(
        "vista_updates",
        "App Updates & Security",
        NotificationManager.IMPORTANCE_HIGH
    ).apply { description = "Essential Vista Watch releases and critical security updates" }

    val promotionsChannel = NotificationChannel(
        "vista_promotions",
        "Promotions & Recommendations",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "New content recommendations, feature announcements, and milestone achievements" }

    manager.createNotificationChannel(updatesChannel)
    manager.createNotificationChannel(promotionsChannel)
}
```

---

## 🗄️ 6. Cloud Sync Outbox Entity (`data/local/entity/SyncOutboxEntity.kt`)

**Proposed design only, not implemented cloud sync.** Shipping this requires an upgrade-safe Room migration, DAO, authenticated server contract, owner isolation, deletion/conflict semantics, a bounded retry worker and tests. Adding the entity alone does not synchronize a watchlist or history.

```kotlin
// Location: com.example.vista.data.local.entity.SyncOutboxEntity

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerId: String,
    val entityType: String, // "WATCHLIST", "HISTORY", "PREFERENCES"
    val entityId: String,
    val action: String, // "UPSERT", "DELETE"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)
```
```
---

## 📲 7. FCM Announcement Data Payload Example (`send_festive_fcm.mjs`)

Historical payload example only. The 1.0.10 announcement has already been sent; do not resend it as a test. Current senders must validate the actual published version and use Vista Watch branding.

```javascript
const message = {
  topic: 'vista_updates',
  data: {
    title: 'Vista 1.0.10 is coming ✨',
    body: 'Wishing you a wonderful start to the festive season! We’re preparing Vista 1.0.10 with festive picks and better movie suggestions.',
    category: 'promotions',
    check_update: 'false',
    announcement_id: 'festive-welcome-2026'
  },
  android: {
    priority: 'NORMAL',
    ttl: '86400s',
    collapse_key: 'festive-welcome-2026'
  }
};
```
