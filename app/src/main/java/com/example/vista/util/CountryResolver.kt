package com.example.vista.util

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Works out which country a viewer is in and maps it to the broadcast topics that install should
 * receive.
 *
 * Country is used for two things: keeping announcements market-appropriate (pricing, availability,
 * language), and, once the referral backend lands, the rules attached to a reward. It is read from
 * the device rather than asked for, because a country prompt on first launch is pure conversion tax
 * and this data is neither sensitive nor something the viewer is likely to get wrong.
 */
object CountryResolver {

    /** South Africa - the primary market, and the one the referral campaign is built for. */
    const val SOUTH_AFRICA = "ZA"

    /**
     * The market-specific broadcast topic for a country code.
     *
     * Deliberately only two buckets. South Africa versus everyone else is as fine as this needs to
     * be: narrower tiers would split the audience below a size where a push is worth sending, and
     * every extra topic is another send that can be aimed at the wrong list.
     */
    fun marketTopic(countryCode: String): String =
        if (countryCode.trim().uppercase(Locale.ROOT) == SOUTH_AFRICA) TOPIC_SOUTH_AFRICA else TOPIC_REST_OF_WORLD

    /**
     * Best guess at the viewer's ISO 3166-1 alpha-2 country code, or "" when it cannot be
     * determined.
     *
     * The configured locale wins over the SIM country. Per-app and per-system language overrides are
     * common, so a South African handset may well report en-GB - and a viewer is far more likely to
     * have a sensible language region than a SIM that matches where they actually live. The SIM is
     * still worth consulting as a fallback, but on its own it is absent from every TV install, which
     * is a large slice of the audience.
     */
    fun resolve(context: Context): String {
        fromConfiguration(context)?.let { return it }
        fromNetwork(context)?.let { return it }
        return ""
    }

    private fun fromConfiguration(context: Context): String? {
        // Indexed rather than iterated: LocaleList is not a Kotlin collection, so it has no
        // asSequence(). Checking in priority order matters - a viewer may set a regional language
        // (en-ZA) ahead of their base one, and the first match with a real country wins.
        val locales = context.resources.configuration.locales
        for (index in 0 until locales.size()) {
            val code = locales[index].country.uppercase(Locale.ROOT)
            if (isCountryCode(code)) return code
        }
        return null
    }

    private fun fromNetwork(context: Context): String? = try {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        telephony?.networkCountryIso?.uppercase(Locale.ROOT)?.takeIf { isCountryCode(it) }
    } catch (_: Exception) {
        // Network country is unavailable without the READ_PHONE_STATE permission, and on TV.
        null
    }

    /** Guards against the empty region on a bare language locale and the "00" that unknown SIMs report. */
    private fun isCountryCode(value: String): Boolean = value.length == 2 && value != "00"

    const val TOPIC_SOUTH_AFRICA = "vista_updates_za"
    const val TOPIC_REST_OF_WORLD = "vista_updates_row"
}
