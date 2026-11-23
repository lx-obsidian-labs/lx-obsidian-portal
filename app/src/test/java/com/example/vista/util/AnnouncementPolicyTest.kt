package com.example.vista.util

import org.junit.Assert.*
import org.junit.Test

class AnnouncementPolicyTest {
    @Test fun obsoleteReleaseIsRemovedButCurrentUpcomingReleaseAndGeneralMessagesRemain() {
        assertTrue(AnnouncementPolicy.obsoleteRelease("Vista 1.0.9 is coming", "Update soon", "1.0.10"))
        assertTrue(AnnouncementPolicy.obsoleteRelease("Vista Cinema 1.0.9 — Update required", "Install now", "1.0.10"))
        assertFalse(AnnouncementPolicy.obsoleteRelease("Vista 1.0.10 is coming", "Stay tuned", "1.0.10"))
        assertFalse(AnnouncementPolicy.obsoleteRelease("Vista 1.0.11 is coming", "Stay tuned", "1.0.10"))
        assertFalse(AnnouncementPolicy.obsoleteRelease("Wonderful festive season", "Great movie nights", "1.0.10"))
    }
    @Test fun versionNineDoesNotAccidentallyMatchNinety() {
        assertFalse(AnnouncementPolicy.obsoleteRelease("Vista 1.0.90 update", "Available", "1.0.10"))
    }
}
