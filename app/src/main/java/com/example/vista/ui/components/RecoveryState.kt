package com.example.vista.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vista.ui.theme.*

/** Shared recovery layout: readable on narrow screens and scrollable in landscape/large text. */
@Composable
fun RecoveryState(
    title: String,
    message: String,
    icon: ImageVector,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    detail: String? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp), color = CardSurface,
                border = BorderStroke(1.dp, PrimaryText.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.background(Brush.verticalGradient(listOf(RaisedSurface, CardSurface)))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(shape = CircleShape, color = Accent.copy(alpha = 0.12f)) {
                        Icon(icon, contentDescription = null, tint = AccentLight,
                            modifier = Modifier.padding(18.dp).size(30.dp))
                    }
                    Text(title, color = PrimaryText, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(message, color = SecondaryText, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center)
                    detail?.let {
                        Text(it, color = SecondaryText, style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center)
                    }
                    Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = PrimaryText)) {
                        Text(primaryLabel, fontWeight = FontWeight.SemiBold)
                    }
                    if (secondaryLabel != null && onSecondary != null) {
                        TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(secondaryLabel, color = SecondaryText)
                        }
                    }
                }
            }
        }
    }
}
