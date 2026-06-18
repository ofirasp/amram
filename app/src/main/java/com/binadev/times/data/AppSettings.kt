package com.binadev.times.data

import android.content.Context
import org.json.JSONArray

enum class PrayerSystem(val hebrewName: String) {
    MIZRACHI("מזרחי"),
    ASHKENAZ("אשכנז"),
}

enum class PrayerTimeMode { MANUAL, HANETZ }

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
    val showMoedSlides: Boolean = true,
    val showYahrtzeitSlides: Boolean = true,
    val shacharitWeekdayMode: PrayerTimeMode = PrayerTimeMode.MANUAL,
    val shacharitWeekdayTime: String = "5:45",
    val shacharitWeekdayOffset: Int = 0,
    val minchaWeekdayOffset: Int = -30,
    val shacharitShabbatMode: PrayerTimeMode = PrayerTimeMode.MANUAL,
    val shacharitShabbatTime: String = "7:30",
    val shacharitShabbatOffset: Int = 0,
    val kabbalatShabbatOffset: Int = 0,
    val minchaShabbatOffset: Int = -30,
    val arvitShabbatOffset: Int = 0,
    val nightDimEnabled: Boolean = true,
    val nightDimStart: Int = 22,
    val nightDimEnd: Int = 4,
    val selichotOffset: Int = 60,
)

object SettingsStore {
    private const val PREFS = "synagogue_settings"
    private const val KEY_CITY = "city_index"
    private const val KEY_SYSTEM = "prayer_system"
    private const val KEY_ANNOUNCEMENTS = "announcements"
    private const val KEY_TEST_DATE_TIME = "test_date_time"
    private const val KEY_TITLE_LINE1 = "title_line1"
    private const val KEY_TITLE_LINE2 = "title_line2"
    private const val KEY_SHOW_MOED    = "show_moed"
    private const val KEY_SHOW_YAHRZEITS = "show_yahrzeits"
    private const val KEY_SHA_WD_MODE   = "sha_wd_mode"
    private const val KEY_SHA_WD_TIME   = "sha_wd_time"
    private const val KEY_SHA_WD_OFFSET = "sha_wd_offset"
    private const val KEY_MIN_WD_OFFSET = "min_wd_offset"
    private const val KEY_SHA_SH_MODE   = "sha_sh_mode"
    private const val KEY_SHA_SH_TIME   = "sha_sh_time"
    private const val KEY_SHA_SH_OFFSET = "sha_sh_offset"
    private const val KEY_KAB_SH_OFFSET    = "kab_sh_offset"
    private const val KEY_MIN_SH_OFFSET   = "min_sh_offset"
    private const val KEY_ARVIT_SH_OFFSET = "arvit_sh_offset"
    private const val KEY_NIGHT_DIM_ENABLED = "night_dim_enabled"
    private const val KEY_NIGHT_DIM_START  = "night_dim_start"
    private const val KEY_NIGHT_DIM_END    = "night_dim_end"
    private const val KEY_SELICHOT_OFFSET  = "selichot_offset"

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
            .putBoolean(KEY_SHOW_MOED, settings.showMoedSlides)
            .putBoolean(KEY_SHOW_YAHRZEITS, settings.showYahrtzeitSlides)
            .putString(KEY_SHA_WD_MODE,   settings.shacharitWeekdayMode.name)
            .putString(KEY_SHA_WD_TIME,   settings.shacharitWeekdayTime)
            .putInt(KEY_SHA_WD_OFFSET,    settings.shacharitWeekdayOffset)
            .putInt(KEY_MIN_WD_OFFSET,    settings.minchaWeekdayOffset)
            .putString(KEY_SHA_SH_MODE,   settings.shacharitShabbatMode.name)
            .putString(KEY_SHA_SH_TIME,   settings.shacharitShabbatTime)
            .putInt(KEY_SHA_SH_OFFSET,    settings.shacharitShabbatOffset)
            .putInt(KEY_KAB_SH_OFFSET,    settings.kabbalatShabbatOffset)
            .putInt(KEY_MIN_SH_OFFSET,    settings.minchaShabbatOffset)
            .putInt(KEY_ARVIT_SH_OFFSET,  settings.arvitShabbatOffset)
            .putBoolean(KEY_NIGHT_DIM_ENABLED, settings.nightDimEnabled)
            .putInt(KEY_NIGHT_DIM_START,  settings.nightDimStart)
            .putInt(KEY_NIGHT_DIM_END,    settings.nightDimEnd)
            .putInt(KEY_SELICHOT_OFFSET,  settings.selichotOffset)
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
        val showMoedSlides = prefs.getBoolean(KEY_SHOW_MOED, true)
        val showYahrtzeitSlides = prefs.getBoolean(KEY_SHOW_YAHRZEITS, true)
        fun loadMode(key: String) = runCatching {
            PrayerTimeMode.valueOf(prefs.getString(key, "") ?: "")
        }.getOrDefault(PrayerTimeMode.MANUAL)
        return AppSettings(
            city = PresetCities[idx],
            prayerSystem = system,
            announcements = announcements,
            testDateTime = testDateTime,
            titleLine1 = titleLine1,
            titleLine2 = titleLine2,
            showMoedSlides = showMoedSlides,
            showYahrtzeitSlides = showYahrtzeitSlides,
            shacharitWeekdayMode   = loadMode(KEY_SHA_WD_MODE),
            shacharitWeekdayTime   = prefs.getString(KEY_SHA_WD_TIME, null) ?: "5:45",
            shacharitWeekdayOffset = prefs.getInt(KEY_SHA_WD_OFFSET, 0),
            minchaWeekdayOffset    = prefs.getInt(KEY_MIN_WD_OFFSET, -30),
            shacharitShabbatMode   = loadMode(KEY_SHA_SH_MODE),
            shacharitShabbatTime   = prefs.getString(KEY_SHA_SH_TIME, null) ?: "7:30",
            shacharitShabbatOffset = prefs.getInt(KEY_SHA_SH_OFFSET, 0),
            kabbalatShabbatOffset  = prefs.getInt(KEY_KAB_SH_OFFSET, 0),
            minchaShabbatOffset    = prefs.getInt(KEY_MIN_SH_OFFSET, -30),
            arvitShabbatOffset     = prefs.getInt(KEY_ARVIT_SH_OFFSET, 0),
            nightDimEnabled        = prefs.getBoolean(KEY_NIGHT_DIM_ENABLED, true),
            nightDimStart          = prefs.getInt(KEY_NIGHT_DIM_START, 22),
            nightDimEnd            = prefs.getInt(KEY_NIGHT_DIM_END, 4),
            selichotOffset         = prefs.getInt(KEY_SELICHOT_OFFSET, 60),
        )
    }
}
