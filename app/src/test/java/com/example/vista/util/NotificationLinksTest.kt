package com.example.vista.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationLinksTest {

    @Test fun allowsOfficialHttpsDestinations() {
        assertEquals("https://www.lxobsidianportal.co.za/insider",
            NotificationLinks.resolve("https://www.lxobsidianportal.co.za/insider"))
        assertEquals("https://play.google.com/apps/testing/com.obsidianlabs.lxpocketbusiness",
            NotificationLinks.resolve("https://play.google.com/apps/testing/com.obsidianlabs.lxpocketbusiness"))
        assertEquals("https://www.lxobsidianportal.co.za/insider",
            NotificationLinks.resolve("  https://WWW.LXOBSIDIANPORTAL.CO.ZA/insider  "))
    }

    @Test fun rejectsUntrustedOrMalformedLinks() {
        listOf(
            null, "", "   ",
            "http://www.lxobsidianportal.co.za/insider",
            "javascript:alert(1)",
            "https://evil.example/insider",
            "https://evil.lxobsidianportal.co.za/insider",
            "https://lxobsidianportal.co.za.evil.example/",
            "https://user:pass@www.lxobsidianportal.co.za/insider",
            "https://www.lxobsidianportal.co.za:8443/insider",
            "https://www.lxobsidianportal.co.za/insider\nX-Injected: 1",
            "not a url"
        ).forEach { assertNull("Expected rejection for $it", NotificationLinks.resolve(it)) }
    }
}
