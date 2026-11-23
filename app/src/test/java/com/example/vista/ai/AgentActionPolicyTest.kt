package com.example.vista.ai

import org.junit.Assert.*
import org.junit.Test

class AgentActionPolicyTest {
    @Test fun entertainmentNewsUsesGroundedConversationWithoutStealingTitleActions() {
        assertTrue(AgentActionPolicy.isNewsRequest("Latest entertainment news"))
        assertEquals("help", AgentActionPolicy.localPlan("Latest entertainment news").tool)
        assertFalse(AgentActionPolicy.isNewsRequest("Find News of the World"))
        assertFalse(AgentActionPolicy.isNewsRequest("Tell me more"))
        assertTrue(AgentActionPolicy.isNewsRequest("What is confirmed in these reports?", listOf(ChatMessage("user", "Latest entertainment news"))))
        assertTrue(AgentActionPolicy.isNewsFollowup("Explain the second one", listOf(ChatMessage("user", "Latest entertainment news"))))
        assertFalse(AgentActionPolicy.isNewsFollowup("Explain the second one", listOf(ChatMessage("user", "Recommend a comedy"))))
    }
    @Test fun explicitFollowupIsTypedWithoutInventingTitleIds() {
        val plan = AgentActionPolicy.localPlan("Download the second one")
        assertEquals("download", plan.tool)
        assertEquals(2, plan.selection)
        assertEquals("save", AgentActionPolicy.localPlan("save the first movie").tool)
        assertTrue(AgentActionPolicy.referencesPrevious("download that one"))
    }
    @Test fun titlesContainingOrdinalsAreNotTreatedAsNumberedReferences() {
        assertNull(AgentActionPolicy.localPlan("download First Contact").selection)
        assertNull(AgentActionPolicy.localPlan("search The Third Man").selection)
    }
    @Test fun personalLibraryAndResumeUseDedicatedTools() {
        assertEquals("watchlist", AgentActionPolicy.localPlan("show my watchlist").tool)
        assertEquals("continue", AgentActionPolicy.localPlan("resume watching").tool)
        assertEquals("compare", AgentActionPolicy.localPlan("compare Inception vs Interstellar").tool)
        assertFalse("execute" in AgentActionPolicy.tools)
    }
}
