package com.example.vista.util

import org.junit.Assert.assertEquals
import org.junit.Test

class VistaBrandTest {
    @Test fun normalizesLegacyPushAndInboxLabelsWithoutBreakingInstallerLinks() {
        val url = "https://example.test/VistaCinema-v1.0.11.apk"
        assertEquals("Vista Watch is ready. $url", VistaBrand.normalize("Vista Cinema is ready. $url"))
        assertEquals("VISTA WATCH", VistaBrand.normalize("VISTA CINEMA"))
    }
}
