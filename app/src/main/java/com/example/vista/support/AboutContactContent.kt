package com.example.vista.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vista.ui.theme.*

@Composable
fun AboutContactContent(onOpenSupport: () -> Unit) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var showLicenses by remember { mutableStateOf(false) }
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Developed and maintained by ${VistaContacts.COMPANY}", color = PrimaryText, fontWeight = FontWeight.SemiBold)
        Text(VistaContacts.SUPPORT, color = SecondaryText)
        ContactButton("Help & Support", onOpenSupport, primary = true)
        ContactButton("Copy technical support email", { status = if (SupportActions.copy(context, VistaContacts.SUPPORT)) "Support email copied." else "Clipboard unavailable. ${VistaContacts.SUPPORT}" })
        ContactButton("Official website", { if (!SupportActions.open(context, VistaContacts.WEBSITE)) status = "No browser is available. ${VistaContacts.WEBSITE}" })
        ContactButton("Privacy policy", { if (!SupportActions.open(context, VistaContacts.PRIVACY_POLICY)) status = "No browser is available. ${VistaContacts.PRIVACY_POLICY}" })
        ContactButton("Terms of service", { if (!SupportActions.open(context, VistaContacts.TERMS)) status = "No browser is available. ${VistaContacts.TERMS}" })
        ContactButton("Business partnerships", {
            val opened = SupportActions.compose(context, SupportEmail(VistaContacts.PARTNERSHIPS, "Vista Watch | Partnership enquiry", "Describe your proposed collaboration.\n\n"))
            status = if (opened) "Email app opened. Review and send your enquiry there."
                else "No email app is available. ${VistaContacts.PARTNERSHIPS}"
        })
        ContactButton("Copy partnership email", { status = if (SupportActions.copy(context, VistaContacts.PARTNERSHIPS)) "Partnership email copied." else "Clipboard unavailable. ${VistaContacts.PARTNERSHIPS}" })
        ContactButton("Open-source licences and third-party notices", { showLicenses = true })
        Text("© 2026 ${VistaContacts.COMPANY}. All rights reserved.", color = SecondaryText)
        status?.let { Text(it, color = SecondaryText) }
    }
    if (showLicenses) AlertDialog(onDismissRequest = { showLicenses = false }, containerColor = CardSurface,
        title = { Text("Licences & third-party notices", color = PrimaryText) },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("AndroidX/Jetpack, Kotlin, Coroutines, Dagger/Hilt, OkHttp, Retrofit, Gson and Coil include open-source components. Firebase and Google Mobile Ads also have applicable vendor terms.", color = SecondaryText)
            listOf("AndroidX — Apache 2.0" to "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt",
                "Kotlin — Apache 2.0" to "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt",
                "OkHttp / Retrofit — Apache 2.0" to "https://github.com/square/okhttp/blob/master/LICENSE.txt",
                "Coil — Apache 2.0" to "https://github.com/coil-kt/coil/blob/main/LICENSE.txt",
                "Firebase terms" to "https://firebase.google.com/terms",
                "Google Mobile Ads terms" to "https://developers.google.com/admob/terms").forEach { (label, url) ->
                ContactButton(label, { if (!SupportActions.open(context, url)) status = "No browser is available to open this notice." })
            }
        } }, confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("Close") } })
}
