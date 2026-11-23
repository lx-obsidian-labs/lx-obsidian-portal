package com.example.vista.support

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.vista.BuildConfig
import com.example.vista.ui.components.PremiumPlan
import com.example.vista.ui.theme.*
import com.example.vista.util.LocalTvConfiguration

@Composable
fun SupportScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(if (LocalTvConfiguration.current.isTv) 32.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ContactButton("Back", onBack)
                Text("Help & Support", color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).widthIn(max = 1000.dp).align(Alignment.CenterHorizontally)) {
                SupportContent()
            }
        }
    }
}

@Composable
fun SupportContent() {
    val context = LocalContext.current
    var reportOpen by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf<SupportEmail?>(null) }
    var unavailable by remember { mutableStateOf<SupportEmail?>(null) }
    var assistance by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    fun compose(email: SupportEmail) {
        if (SupportActions.compose(context, email)) status = "Email app opened. Review and send your message there."
        else unavailable = email
    }
    fun website(url: String) {
        if (!SupportActions.open(context, url)) status = "No browser could open this link. You can copy the support email below."
    }

    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("How can we help?", color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Support for Vista Watch, developed and maintained by ${VistaContacts.COMPANY}.", color = SecondaryText)
        Text(VistaContacts.SUPPORT, color = PrimaryText, style = MaterialTheme.typography.titleMedium)
        if (com.example.vista.util.LocalTvConfiguration.current.isTv) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.example.vista.R.drawable.vista_support_qr),
                    contentDescription = "Scan the official Vista Watch support page", modifier = Modifier.size(180.dp))
                Column {
                    Text("Continue on your phone", color = PrimaryText, fontWeight = FontWeight.Bold)
                    Text("This QR contains only our public support URL, with no account or device information.", color = SecondaryText)
                    Text(VistaContacts.SUPPORT_PAGE, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        ContactButton("Report a problem", { reportOpen = true }, primary = true)
        ContactButton("Playback and streaming assistance", { assistance = "Playback and streaming assistance" })
        ContactButton("Installation and update assistance", { assistance = "Installation and update assistance" })
        ContactButton("Account assistance", { assistance = "Account assistance" })
        ContactButton("Suggest a feature", { compose(SupportEmail(VistaContacts.SUPPORT,
            "Vista Watch | Feature Suggestion | ${BuildConfig.VERSION_NAME}", "What would you like Vista Watch to do better?\n\n")) })
        ContactButton("Contact technical support", { compose(SupportEmail(VistaContacts.SUPPORT,
            "Vista Watch | Support | ${BuildConfig.VERSION_NAME}", "How can we help?\n\n")) })
        ContactButton("Copy support email", { status = if (SupportActions.copy(context, VistaContacts.SUPPORT)) "Support email copied." else "Clipboard unavailable. ${VistaContacts.SUPPORT}" })
        ContactButton("Open browser-based support", { website(VistaContacts.SUPPORT_PAGE) })
        ContactButton("Frequently asked questions", { assistance = "Frequently asked questions" })
        ContactButton("Visit official website", { website(VistaContacts.WEBSITE) })
        status?.let { Text(it, color = SecondaryText, style = MaterialTheme.typography.bodyMedium) }
        HorizontalDivider(color = MutedText.copy(alpha = 0.25f))
        PrivacyLegalContent()
        if (PremiumPlan.isLive()) {
            ContactButton("Billing and subscription support", { compose(SupportEmail(VistaContacts.BILLING,
                "Vista Watch | Billing enquiry", "Describe your billing enquiry. Do not include card details or payment passwords.\n\n")) })
        }
    }

    if (reportOpen) BugReportEditor(onDismiss = { reportOpen = false }, onReview = { preview = it })
    preview?.let { email ->
        EmailPreviewDialog(email, onEdit = { preview = null }, onCompose = { preview = null; reportOpen = false; compose(email) })
    }
    unavailable?.let { email ->
        AlertDialog(onDismissRequest = { unavailable = null }, containerColor = CardSurface,
            title = { Text("No email app is available", color = PrimaryText) },
            text = { Text("Your message has not been sent. Copy the address/report or open the support page in a browser.\n\n${email.recipient}", color = SecondaryText) },
            confirmButton = { TextButton(onClick = { val copied = SupportActions.copy(context, "To: ${email.recipient}\nSubject: ${email.subject}\n\n${email.body}"); status = if (copied) "Report copied. Nothing has been sent." else "Clipboard unavailable. Use the support website or ${email.recipient}."; unavailable = null }) { Text("Copy report") } },
            dismissButton = { TextButton(onClick = { website(VistaContacts.SUPPORT_PAGE); unavailable = null }) { Text("Open support page") } })
    }
    assistance?.let { title ->
        val message = when (title) {
            "Playback and streaming assistance" -> "Check your connection, then try another source from the player's source settings. On TV, press Menu or hold OK for playback sources; Back returns to the video. Provider players and native direct streams have different controls. Automatic next episode uses confirmed direct-stream completion. No source is guaranteed to carry every title."
            "Installation and update assistance" -> "Use Settings → About → Check for updates, or the official Vista download page. Install updates over the existing app to retain local data. Android may ask you to permit installation from your chosen source. Do not uninstall as the first troubleshooting step. iPhone developer builds require Apple signing before installation."
            "Account assistance" -> "Use a complete email address and the password you registered. Forgot password requests a reset link without revealing whether an account exists. If profile loading or setup fails after sign-in, retry that step instead of creating another account. Contact technical support if access remains unavailable. Never include a password in a report."
            else -> "Saved titles are metadata, not offline downloads. Completed eligible downloads play from Library → Downloads; embedded sources may be streaming-only. Your library remains on this device unless an explicitly supported feature says otherwise. The current app version is ${BuildConfig.VERSION_NAME}."
        }
        AlertDialog(onDismissRequest = { assistance = null }, containerColor = CardSurface,
            title = { Text(title, color = PrimaryText) }, text = { Text(message, color = SecondaryText) },
            confirmButton = { TextButton(onClick = { assistance = null }) { Text("Got it") } })
    }
}

@Composable
fun PrivacyLegalContent() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    Text("Privacy & Legal", color = PrimaryText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text("Privacy, personal information, deletion requests, rights and copyright complaints: ${VistaContacts.PRIVACY}", color = SecondaryText)
    ContactButton("Contact privacy and legal", {
        val opened = SupportActions.compose(context, SupportEmail(VistaContacts.PRIVACY, "Vista Watch | Privacy / Legal enquiry", "Describe your request. Do not include passwords or payment information.\n\n"))
        status = if (opened) "Email draft opened. Your request has not been submitted until you send it." else "No email app is available. Copy the address or use the website."
    })
    ContactButton("Copy privacy email", { status = if (SupportActions.copy(context, VistaContacts.PRIVACY)) "Privacy email copied." else "Clipboard unavailable. ${VistaContacts.PRIVACY}" })
    ContactButton("Privacy policy", { if (!SupportActions.open(context, VistaContacts.PRIVACY_POLICY)) status = "No browser is available to open the privacy policy." })
    ContactButton("Terms of service", { if (!SupportActions.open(context, VistaContacts.TERMS)) status = "No browser is available to open the terms." })
    status?.let { Text(it, color = SecondaryText) }
}

@Composable
fun ContactButton(label: String, onClick: () -> Unit, primary: Boolean = false, enabled: Boolean = true) {
    var focused by remember { mutableStateOf(false) }
    val modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).onFocusChanged { focused = it.isFocused }
        .border(if (focused) 3.dp else 0.dp, if (focused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
    if (primary) Button(onClick, modifier, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(12.dp)) { Text(label, color = PrimaryText) }
    else OutlinedButton(onClick, modifier, enabled = enabled, shape = RoundedCornerShape(12.dp)) { Text(label, color = PrimaryText) }
}

@Composable
private fun BugReportEditor(onDismiss: () -> Unit, onReview: (SupportEmail) -> Unit) {
    val context = LocalContext.current
    val categoryDefault = SupportCategory.OTHER.name
    var categoryName by rememberSaveable { mutableStateOf(categoryDefault) }
    var description by rememberSaveable { mutableStateOf("") }
    var includeDiagnostics by rememberSaveable { mutableStateOf(false) }
    val deviceCategory = com.example.vista.util.LocalDeviceType.current.name
    val category = SupportCategory.valueOf(categoryName)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardSurface, shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(20.dp).widthIn(max = 800.dp).fillMaxWidth().heightIn(max = 680.dp).imePadding()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Report a problem", color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Nothing is sent automatically. You will review the complete email before opening an email app.", color = SecondaryText)
                Column(Modifier.selectableGroup()) {
                    SupportCategory.entries.forEach { option ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = category == option, role = Role.RadioButton,
                            onClick = { categoryName = option.name }), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(category == option, onClick = null)
                            Text(option.label, color = PrimaryText, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                OutlinedTextField(description, { description = it.take(SupportReport.MAX_DESCRIPTION) }, label = { Text("What happened?") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3, supportingText = { Text("${description.length}/${SupportReport.MAX_DESCRIPTION}. Leave out passwords, tokens, card details and private history.") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(includeDiagnostics, onCheckedChange = { includeDiagnostics = it })
                    Text("Include app version, OS version, manufacturer/model and device category. (Optional)", color = SecondaryText)
                }
                ContactButton("Review report", {
                    onReview(SupportReport.build(category, description, BuildConfig.VERSION_NAME, includeDiagnostics,
                        SupportDiagnostics("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", "Android ${Build.VERSION.RELEASE}",
                            Build.MANUFACTURER, Build.MODEL, deviceCategory)))
                }, primary = true, enabled = description.isNotBlank())
                if (description.isBlank()) Text("Describe the problem before reviewing your report.", color = SecondaryText)
                ContactButton("Cancel", onDismiss)
            }
        }
    }
}

@Composable
private fun EmailPreviewDialog(email: SupportEmail, onEdit: () -> Unit, onCompose: () -> Unit) {
    Dialog(onDismissRequest = onEdit, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = CardSurface, shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(20.dp).widthIn(max = 800.dp).fillMaxWidth().heightIn(max = 680.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Review your report", color = PrimaryText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("To: ${email.recipient}\nSubject: ${email.subject}", color = PrimaryText)
                Text(email.body, color = SecondaryText)
                Text("Opening your email app does not send this report. You can edit and send it there.", color = SecondaryText)
                ContactButton("Open email app", onCompose, primary = true)
                ContactButton("Edit report", onEdit)
            }
        }
    }
}
