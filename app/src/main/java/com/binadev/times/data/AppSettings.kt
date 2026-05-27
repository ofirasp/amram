package com.binadev.times.data

import android.content.Context
import org.json.JSONArray

enum class PrayerSystem(val hebrewName: String) {
    MIZRACHI("מזרחי"),
    ASHKENAZ("אשכנז"),
}

data class CityLocation(
    val nameHebrew: String,
    val nameEnglish: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 30.0,
    val timeZoneId: String = "Asia/Jerusalem",
)

val PresetCities = listOf(
    CityLocation("חיפה",      "Haifa",      32.794, 34.989, 20.0),
    CityLocation("ירושלים",  "Jerusalem",  31.769, 35.216, 786.0),
    CityLocation("תל אביב",  "Tel Aviv",   32.087, 34.800, 5.0),
    CityLocation("באר שבע",  "Beer Sheva", 31.252, 34.791, 280.0),
    CityLocation("אשדוד",    "Ashdod",     31.804, 34.650, 30.0),
    CityLocation("נתניה",    "Netanya",    32.329, 34.860, 30.0),
    CityLocation("בני ברק",  "Bnei Brak",  32.084, 34.836, 20.0),
    CityLocation("רחובות",   "Rehovot",    31.894, 34.809, 55.0),
)

val defaultAnnouncements = listOf(
    "אם פה נדבר, איפה נתפלל?",
    "שיעור הדף היומי מפי הרב מיכאל זק בכל יום בשעה 19:30 דרך אפליקציית זום, לינק נשלח בקבוצת הוואצאפ של ביהכנ\"ס.",
)

data class AppSettings(
    val city: CityLocation = PresetCities[0],
    val prayerSystem: PrayerSystem = PrayerSystem.MIZRACHI,
    val announcements: List<String> = defaultAnnouncements,
    val testDateTime: Long? = null,
    val titleLine1: String = "בית הכנסת היכל עמרם",
    val titleLine2: String = "ואני ברב חסדך אבוא ביתך אשתחווה אל היכל קדשך ביראתך",
)

object SettingsStore {
    private const val PREFS = "synagogue_settings"
    private const val KEY_CITY = "city_index"
    private const val KEY_SYSTEM = "prayer_system"
    private const val KEY_ANNOUNCEMENTS = "announcements"
    private const val KEY_TEST_DATE_TIME = "test_date_time"
    private const val KEY_TITLE_LINE1 = "title_line1"
    private const val KEY_TITLE_LINE2 = "title_line2"

    fun save(context: Context, settings: AppSettings) {
        val idx = PresetCities.indexOfFirst { it.nameEnglish == settings.city.nameEnglish }.coerceAtLeast(0)
        val announcementsJson = JSONArray().apply { settings.announcements.forEach { put(it) } }.toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_CITY, idx)
            .putString(KEY_SYSTEM, settings.prayerSystem.name)
            .putString(KEY_ANNOUNCEMENTS, announcementsJson)
            .putLong(KEY_TEST_DATE_TIME, settings.testDateTime ?: -1L)
            .putString(KEY_TITLE_LINE1, settings.titleLine1)
            .putString(KEY_TITLE_LINE2, settings.titleLine2)
            .apply()
    }

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val idx = prefs.getInt(KEY_CITY, 0).coerceIn(0, PresetCities.lastIndex)
        val system = runCatching {
            PrayerSystem.valueOf(prefs.getString(KEY_SYSTEM, "") ?: "")
        }.getOrDefault(PrayerSystem.MIZRACHI)
        val announcements = prefs.getString(KEY_ANNOUNCEMENTS, null)?.let { json ->
            runCatching {
                val arr = JSONArray(json)
                (0 until arr.length()).map { arr.getString(it) }
            }.getOrDefault(defaultAnnouncements)
        } ?: defaultAnnouncements
        val testDateTime = prefs.getLong(KEY_TEST_DATE_TIME, -1L).let { if (it == -1L) null else it }
        val titleLine1 = prefs.getString(KEY_TITLE_LINE1, null) ?: "בית הכנסת היכל עמרם"
        val titleLine2 = prefs.getString(KEY_TITLE_LINE2, null) ?: "ואני ברב חסדך אבוא ביתך אשתחווה אל היכל קדשך ביראתך"
        return AppSettings(PresetCities[idx], system, announcements, testDateTime, titleLine1, titleLine2)
    }
}
