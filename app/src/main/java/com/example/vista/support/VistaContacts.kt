package com.example.vista.support

/** Official Vista Watch correspondence configuration. Maintained by LX Obsidian Labs. */
object VistaContacts {
    const val COMPANY = "LX Obsidian Labs"
    const val SUPPORT = "support@lxobsidianportal.co.za"
    const val GENERAL = "info@lxobsidianportal.co.za"
    const val PRIVACY = GENERAL
    const val BILLING = "accounts@lxobsidianportal.co.za"
    const val PARTNERSHIPS = "partnerships@lxobsidianportal.co.za"
    const val EXECUTIVE = "nathan@lxobsidianportal.co.za"
    const val SALES = "sales@lxobsidianportal.co.za"
    const val ADMIN = "admin@lxobsidianportal.co.za"
    const val CAREERS = "careers@lxobsidianportal.co.za"
    const val HELLO = "hello@lxobsidianportal.co.za"
    const val WEBSITE = "https://www.lxobsidianportal.co.za/"
    const val PRODUCT = "https://www.lxobsidianportal.co.za/vista"
    const val SUPPORT_PAGE = "https://www.lxobsidianportal.co.za/vista#vista-support"
    const val PRIVACY_POLICY = "https://www.lxobsidianportal.co.za/privacy-policy"
    const val TERMS = "https://www.lxobsidianportal.co.za/vista-terms"
}

enum class SupportCategory(val label: String) {
    PLAYBACK("Playback problem"), UNAVAILABLE("Stream unavailable"), CRASH("Application crashing"),
    DOWNLOAD("Download failure"), LOGIN("Login problem"), TV("Smart TV navigation problem"),
    PERFORMANCE("Performance problem"), RECOMMENDATIONS("Recommendation issue"), OTHER("Other")
}

/** Explicit allowlist. No IDs, account information, history, logs, tokens or location fields. */
data class SupportDiagnostics(val appVersion: String, val osVersion: String, val manufacturer: String,
    val model: String, val deviceCategory: String)
data class SupportEmail(val recipient: String, val subject: String, val body: String)

object SupportReport {
    const val MAX_DESCRIPTION = 4000
    fun build(category: SupportCategory, description: String, appVersion: String,
        includeDiagnostics: Boolean, diagnostics: SupportDiagnostics?): SupportEmail {
        require(description.isNotBlank()) { "Describe what happened before reviewing your report." }
        require(description.length <= MAX_DESCRIPTION) { "Please keep the description under $MAX_DESCRIPTION characters." }
        fun oneLine(value: String) = value.replace(Regex("[\\r\\n]"), " ").take(160)
        val body = buildString {
            append("Vista Watch bug report\nCategory: ${category.label}\n\nWhat happened:\n${description.trim()}\n")
            if (includeDiagnostics && diagnostics != null) {
                append("\nDiagnostic details included with your permission:\n")
                append("App version: ${oneLine(diagnostics.appVersion)}\n")
                append("OS version: ${oneLine(diagnostics.osVersion)}\n")
                append("Device: ${oneLine(diagnostics.manufacturer)} ${oneLine(diagnostics.model)}\n")
                append("Device category: ${oneLine(diagnostics.deviceCategory)}\n")
            } else append("\nDiagnostic details not included.\n")
        }
        return SupportEmail(VistaContacts.SUPPORT, "Vista Watch | Bug Report | ${category.label} | ${oneLine(appVersion)}", body)
    }
}
