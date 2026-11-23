package com.example.vista.support

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/** User-initiated system actions only. Opening a composer never means a report has been sent. */
object SupportActions {
    fun emailIntent(email: SupportEmail): Intent {
        val uri = Uri.parse("mailto:${email.recipient}?subject=${Uri.encode(email.subject)}&body=${Uri.encode(email.body)}")
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email.recipient))
            putExtra(Intent.EXTRA_SUBJECT, email.subject)
            putExtra(Intent.EXTRA_TEXT, email.body)
        }
    }
    fun compose(context: Context, email: SupportEmail): Boolean = launch(context, emailIntent(email))
    fun open(context: Context, url: String): Boolean {
        if (!url.startsWith("https://")) return false
        return launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    private fun launch(context: Context, intent: Intent): Boolean = try {
        if (intent.resolveActivity(context.packageManager) == null) false else {
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }
    } catch (_: Exception) { false }
    fun copy(context: Context, text: String): Boolean = try {
        val manager = context.getSystemService(ClipboardManager::class.java)
        if (manager == null) false else { manager.setPrimaryClip(ClipData.newPlainText("Vista Watch support", text)); true }
    } catch (_: Exception) { false }
}
