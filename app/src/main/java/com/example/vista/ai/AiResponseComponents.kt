package com.example.vista.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.vista.ui.theme.*

@Composable
fun AiAnswerContent(reply: AssistantReply, onRead: () -> Unit, onFollowup: ((String) -> Unit)?, onSource: ((AssistantSource) -> Unit)? = null) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectionContainer { Text(reply.text, color = PrimaryText, style = MaterialTheme.typography.bodyMedium) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Vista AI answer", reply.text))
                Toast.makeText(context, if (clipboard != null) "Answer copied" else "Clipboard unavailable", Toast.LENGTH_SHORT).show()
            }) { Text("Copy") }
            TextButton(onClick = onRead) { Text("Read aloud") }
        }
        if (reply.sources.isNotEmpty()) {
            HorizontalDivider(color = MutedText.copy(alpha = 0.3f))
            Text("Sources · publisher reporting", color = SecondaryText, style = MaterialTheme.typography.labelMedium)
            reply.retrievedAt?.let { Text("Retrieved ${it.take(16).replace('T', ' ')} UTC", color = SecondaryText, style = MaterialTheme.typography.labelSmall) }
            reply.sources.forEachIndexed { index, source ->
                OutlinedButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                        .onFailure { Toast.makeText(context, "No browser is available", Toast.LENGTH_SHORT).show() }
                }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("[${index + 1}] ${source.title}", color = PrimaryText)
                        Text("${source.publisher} · ${source.publishedAt.take(10)}", color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (onSource != null) TextButton(onClick = { onSource(source) }) { Text("Discuss source ${index + 1}", color = Accent) }
            }
        }
        if (onFollowup != null) reply.followups.forEach { prompt ->
            TextButton(onClick = { onFollowup(prompt) }) { Text(prompt, color = Accent) }
        }
    }
}
