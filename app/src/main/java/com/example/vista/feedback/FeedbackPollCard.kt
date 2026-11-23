package com.example.vista.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vista.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackPollCard(alwaysShow: Boolean = false, onSignIn: () -> Unit = {}, viewModel: FeedbackViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf(false) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    if (alwaysShow || (!state.hidden && !state.sent)) {
        Surface(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), color = CardSurface,
            border = BorderStroke(1.dp, Accent.copy(alpha = 0.25f))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Forum, null, tint = AccentLight)
                    Spacer(Modifier.width(12.dp))
                    Text("Help shape Vista Watch", color = PrimaryText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (!alwaysShow) IconButton(onClick = viewModel::dismiss) { Icon(Icons.Outlined.Close, "Dismiss feedback invitation", tint = SecondaryText) }
                }
                Text("How does the app really feel? Tell us what's working and what needs to change.", color = SecondaryText)
                Text("4 quick questions · Optional · About a minute", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                Button(onClick = { open = true }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text(if (state.sent) "Feedback received" else "Have your say", color = PrimaryText) }
            }
        }
    }
    if (open) {
        ModalBottomSheet(onDismissRequest = { if (!state.sending) open = false },
            containerColor = CardSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (state.sent) "Thanks for being honest" else "Your voice, a better Vista Watch", color = PrimaryText,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (state.sent) {
                    Text("Your response has been received. It helps us choose what to fix, improve and share next.", color = SecondaryText)
                    Button(onClick = { open = false }) { Text("Done") }
                } else {
                    Text("Responses are linked to your account to prevent duplicate votes. Comments stay private unless you choose to allow an anonymous marketing quote.",
                        color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth(), color = Accent)
                    if (step < FeedbackPoll.questions.size) {
                        val question = FeedbackPoll.questions[step]
                        Text("Question ${step + 1} of 4", color = SecondaryText, style = MaterialTheme.typography.labelMedium)
                        Text(question.title, color = PrimaryText, style = MaterialTheme.typography.titleLarge)
                        Column(Modifier.selectableGroup()) {
                            question.options.forEach { option ->
                                val selected = state.answers[question.id] == option.id
                                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                    .selectable(selected = selected, enabled = !state.sending, role = Role.RadioButton,
                                        onClick = { viewModel.answer(question.id, option.id) }).padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selected, onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = Accent))
                                    Spacer(Modifier.width(12.dp))
                                    Text(option.label, color = PrimaryText)
                                }
                            }
                        }
                    } else {
                        Text("Anything else you'd like us to know?", color = PrimaryText, style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(value = state.comment, onValueChange = viewModel::comment, enabled = !state.sending,
                            modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Optional comment") },
                            supportingText = { Text("${state.comment.length}/${FeedbackPoll.MAX_COMMENT} · Please leave out personal information.") })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.quoteAllowed, onCheckedChange = viewModel::quoteAllowed, enabled = !state.sending)
                            Text("You may use my comment anonymously in Vista marketing. (Optional)", color = SecondaryText,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    state.error?.let { Text(it, color = ErrorAccent, style = MaterialTheme.typography.bodyMedium) }
                    if (state.error?.startsWith("Sign in") == true) {
                        OutlinedButton(onClick = { open = false; onSignIn() }) { Text("Sign in", color = PrimaryText) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (step > 0) OutlinedButton(onClick = { step-- }, enabled = !state.sending) { Text("Back", color = PrimaryText) }
                        Button(onClick = { if (step < 4) step++ else viewModel.submit() },
                            enabled = !state.sending && (step == 4 || state.answers.containsKey(FeedbackPoll.questions[step].id)),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.weight(1f)) {
                            if (state.sending) CircularProgressIndicator(Modifier.size(20.dp), color = PrimaryText, strokeWidth = 2.dp)
                            else Text(if (step == 4) "Send feedback" else "Continue", color = PrimaryText)
                        }
                    }
                    TextButton(onClick = { open = false }, enabled = !state.sending) { Text("Not now", color = SecondaryText) }
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}
