package com.example.vista.artwork

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vista.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ArtworkIntegrationTest {
    @Test fun suppliedResourcesDecodeAtTheirDocumentedDimensionsWithoutPortraitStretching() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expected = mapOf(VistaArtwork.ONBOARDING to (941 to 1672), VistaArtwork.MOVIE_NIGHT to (1279 to 720),
            VistaArtwork.CHRISTMAS to (1279 to 720), VistaArtwork.AI_AVATAR to (512 to 512),
            VistaArtwork.MALE_AVATAR to (512 to 512), VistaArtwork.FEMALE_AVATAR to (512 to 512))
        expected.forEach { (artwork, dimensions) ->
            val resource = VistaArtworkRegistry.resolve(context, artwork)
            assertNotNull(artwork.resourceName, resource)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resource!!, bounds)
            assertEquals(dimensions.first, bounds.outWidth)
            assertEquals(dimensions.second, bounds.outHeight)
        }
        assertNull(VistaArtworkRegistry.resolve(context, VistaArtwork.DISCOVER))
    }

    @Test fun avatarIdsPersistAcrossStoreInstancesAndRemainAccountScoped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = PreferencesManager(context)
        val owner = "artwork-test-${UUID.randomUUID()}"
        val guestBefore = first.avatarId("").first()
        first.setAvatarId(owner, "male_01", syncPending = true)
        val reopened = PreferencesManager(context)
        assertEquals("male_01", reopened.avatarId(owner).first())
        assertEquals(guestBefore, reopened.avatarId("").first())
        assertNull(reopened.avatarId("${owner}-other").first())
        reopened.setAvatarId(owner, "female_01", syncPending = true)
        reopened.acknowledgeAvatarSync(owner, "male_01")
        assertTrue(reopened.avatarSyncPending(owner))
        reopened.acknowledgeAvatarSync(owner, "female_01")
        assertFalse(reopened.avatarSyncPending(owner))
    }
}
