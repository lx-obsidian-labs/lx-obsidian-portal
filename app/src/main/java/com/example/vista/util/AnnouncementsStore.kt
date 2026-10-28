package com.example.vista.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device inbox for push announcements received via [com.example.vista.data.remote.firebase.VistaMessagingService].
 *
 * Stored as a small JSON array in SharedPreferences so background messages survive process death and
 * can be rendered in the home-screen notification bell without any network round-trip.
 */
@Singleton
class AnnouncementsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Announcement(
        val title: String,
        val body: String,
        val timestamp: Long
    )

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Adds an announcement at the top of the list, keeping at most [MAX_ENTRIES]. */
    fun add(title: String, body: String, timestamp: Long = System.currentTimeMillis()) {
        val latest = JSONArray()
        val entry = JSONObject()
            .put("title", title)
            .put("body", body)
            .put("ts", timestamp)
        latest.put(entry)

        runCatching {
            val existing = JSONArray(prefs.getString(KEY_ITEMS, "[]").orEmpty())
            var kept = 0
            for (i in 0 until existing.length()) {
                if (kept >= MAX_ENTRIES - 1) break
                latest.put(existing.get(i))
                kept++
            }
        }
        prefs.edit().putString(KEY_ITEMS, latest.toString()).apply()
    }

    fun all(): List<Announcement> {
        val raw = prefs.getString(KEY_ITEMS, "[]").orEmpty()
        val result = mutableListOf<Announcement>()
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                result += Announcement(
                    title = obj.optString("title", "Vista Cinema"),
                    body = obj.optString("body", ""),
                    timestamp = obj.optLong("ts", 0L)
                )
            }
        }
        return result
    }

    /** Newest announcement timestamp, or 0 when the inbox is empty. */
    fun latestTimestamp(): Long = all().firstOrNull()?.timestamp ?: 0L

    fun clear() {
        prefs.edit().remove(KEY_ITEMS).apply()
    }

    companion object {
        const val PREFS_NAME = "vista_bell_inbox"
        private const val KEY_ITEMS = "items"
        private const val MAX_ENTRIES = 20
    }
}