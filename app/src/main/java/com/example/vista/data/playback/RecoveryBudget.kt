package com.example.vista.data.playback

/** One budget per playback request; never bounce indefinitely between sources. */
class RecoveryBudget(private val maximum: Int = 2) {
    private var attempts = 0
    private val failed = mutableSetOf<String>()
    @Synchronized fun take(failedSource: String?): Boolean {
        failedSource?.let(failed::add)
        if (attempts >= maximum) return false
        attempts++
        return true
    }
    @Synchronized fun wasTried(url: String) = url in failed
    @Synchronized fun reset() { attempts = 0; failed.clear() }
}
