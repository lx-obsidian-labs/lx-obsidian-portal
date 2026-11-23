package com.example.vista.ui.settings

import org.junit.Assert.*
import org.junit.Test

class SettingsDirectoryTest {
    @Test fun searchFindsUsefulSectionsByIntentRatherThanOnlyHeading() {
        assertEquals(SettingsPage.DOWNLOADS, SettingsDirectory.search("offline").single().page)
        assertEquals(SettingsPage.AI_VOICE, SettingsDirectory.search("microphone").single().page)
        assertEquals(SettingsPage.DATA_STORAGE, SettingsDirectory.search("cellular cache").single().page)
        assertTrue(SettingsDirectory.search("not-a-setting").isEmpty())
    }
}
