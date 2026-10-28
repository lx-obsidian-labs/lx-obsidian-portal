package com.example.vista.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vista.ui.theme.Accent
import com.example.vista.ui.theme.SecondaryText
import com.example.vista.ui.theme.ThemeColors
import com.example.vista.util.ViewUtils
import kotlinx.coroutines.launch

/**
 * Share targets for a title.
 *
 * A custom sheet rather than the raw chooser because the chooser is a dead end for most of this
 * audience: on a South African handset the installed set is usually WhatsApp and maybe the system
 * share sheet with nothing useful behind it. Naming the targets, with WhatsApp first, is the
 * difference between a share that completes and one that gets abandoned.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    title: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val messenger = rememberVistaMessenger()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val message = remember(title) { ViewUtils.buildShareMessage(title) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ThemeColors.raisedSurface,
        contentColor = ThemeColors.primaryText,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(ThemeColors.primaryText.copy(alpha = 0.25f), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Share \"$title\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ThemeColors.primaryText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
            Text(
                text = "Free for now, no subscription needed",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            ShareTargetRow(
                icon = Icons.AutoMirrored.Outlined.Chat,
                label = "WhatsApp",
                // First because it is the channel this audience actually uses.
                emphasised = true
            ) {
                onDismiss()
                ViewUtils.shareToWhatsApp(context, message)
            }

            ShareTargetRow(icon = Icons.Outlined.NorthEast, label = "X") {
                onDismiss()
                ViewUtils.shareToX(context, message)
            }

            ShareTargetRow(icon = Icons.Outlined.Tag, label = "Facebook") {
                onDismiss()
                ViewUtils.shareToFacebook(context, message)
            }

            ShareTargetRow(icon = Icons.Outlined.ContentCopy, label = "Copy link") {
                clipboard.setText(AnnotatedString(ViewUtils.SITE_URL))
                ViewUtils.shareViaCopyLink(context)
                scope.launch { messenger?.show("Link copied") }
                onDismiss()
            }

            ShareTargetRow(icon = Icons.Outlined.MoreHoriz, label = "More options") {
                onDismiss()
                ViewUtils.shareText(context, message)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareTargetRow(
    icon: ImageVector,
    label: String,
    emphasised: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (emphasised) Accent else ThemeColors.primaryText.copy(alpha = 0.08f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (emphasised) Color.White else ThemeColors.primaryText,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Normal,
            color = ThemeColors.primaryText
        )
    }
}
