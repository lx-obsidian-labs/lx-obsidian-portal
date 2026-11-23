package com.example.vista.ui.tv

import org.junit.Assert.assertEquals
import org.junit.Test

class TvFocusScrollPolicyTest {
    @Test fun focusSettlesAtTheLeadingThird() {
        assertEquals(400f, TvFocusScrollPolicy.distance(700f, 180f, 1000f), 0.01f)
        assertEquals(0f, TvFocusScrollPolicy.distance(300f, 180f, 1000f), 0.01f)
    }
    @Test fun largeFittingItemsKeepTheirTrailingEdgeVisible() {
        assertEquals(300f, TvFocusScrollPolicy.distance(400f, 900f, 1000f), 0.01f)
    }
    @Test fun oversizedItemsAlignToTheLeadingEdgeWithoutImpossibleOffsets() {
        assertEquals(120f, TvFocusScrollPolicy.distance(120f, 1200f, 1000f), 0.01f)
    }
    @Test fun unmeasuredOrInvalidGeometryDoesNotScroll() {
        assertEquals(0f, TvFocusScrollPolicy.distance(0f, 0f, 0f), 0f)
        assertEquals(0f, TvFocusScrollPolicy.distance(Float.NaN, 180f, 1000f), 0f)
    }
}
