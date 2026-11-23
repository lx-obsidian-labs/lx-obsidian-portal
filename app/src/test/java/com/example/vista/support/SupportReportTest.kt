package com.example.vista.support

import org.junit.Assert.*
import org.junit.Test

class SupportReportTest {
    private val details = SupportDiagnostics("1.0.12 (19)", "Android Test OS", "Test Manufacturer", "Test Model", "TV")
    @Test fun diagnosticsRequireAnExplicitOptInAndUseTheOfficialRecipient() {
        val withoutDiagnostics = SupportReport.build(SupportCategory.TV, "Remote focus gets stuck.", "1.0.12", false, details)
        assertEquals(VistaContacts.SUPPORT, withoutDiagnostics.recipient)
        assertEquals("Vista Watch | Bug Report | Smart TV navigation problem | 1.0.12", withoutDiagnostics.subject)
        assertFalse(withoutDiagnostics.body.contains(details.osVersion))
        assertFalse(withoutDiagnostics.body.contains(details.model))
        val consented = SupportReport.build(SupportCategory.TV, "Remote focus gets stuck.", "1.0.12", true, details)
        assertTrue(consented.body.contains(details.osVersion))
        assertTrue(consented.body.contains(details.model))
    }
    @Test fun allConfiguredCorrespondenceUsesTheOfficialDomain() {
        listOf(VistaContacts.SUPPORT, VistaContacts.GENERAL, VistaContacts.PRIVACY, VistaContacts.BILLING,
            VistaContacts.PARTNERSHIPS, VistaContacts.EXECUTIVE, VistaContacts.SALES, VistaContacts.ADMIN,
            VistaContacts.CAREERS, VistaContacts.HELLO).forEach { assertTrue(it.endsWith("@lxobsidianportal.co.za")) }
        assertEquals(VistaContacts.GENERAL, VistaContacts.PRIVACY)
    }
    @Test fun subjectCannotAcquireExtraHeaderLinesAndBlankDescriptionsAreRejected() {
        val report = SupportReport.build(SupportCategory.OTHER, "Example", "1.0.12\nextra", false, null)
        assertFalse(report.subject.contains('\n'))
        try { SupportReport.build(SupportCategory.OTHER, " ", "1", false, null); fail("Expected validation") }
        catch (_: IllegalArgumentException) { }
    }
}
