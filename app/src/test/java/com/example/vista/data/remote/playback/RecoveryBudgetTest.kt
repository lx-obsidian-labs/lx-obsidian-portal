package com.example.vista.data.remote.playback

import com.example.vista.data.playback.RecoveryBudget
import org.junit.Assert.*
import org.junit.Test

class RecoveryBudgetTest {
    @Test fun retriesAreBoundedAcrossDifferentSources() {
        val budget = RecoveryBudget()
        assertTrue(budget.take("a")); assertTrue(budget.take("b")); assertFalse(budget.take("a"))
        assertTrue(budget.wasTried("a")); assertTrue(budget.wasTried("b"))
        budget.reset(); assertFalse(budget.wasTried("a")); assertTrue(budget.take("a"))
    }
}
