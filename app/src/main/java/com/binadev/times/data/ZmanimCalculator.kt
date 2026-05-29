package com.binadev.times.data

import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.text.SimpleDateFormat
import java.util.*

data class DailyCalculations(
    val zmanim: List<ZmanItem> = emptyList(),
    val weekdayPrayers: List<TefilaItem> = emptyList(),
    val shabbatPrayers: List<TefilaItem> = emptyList(),
    val hebrewInfo: HebrewCalendarInfo = HebrewCalendarInfo(),
    val tzaitMs: Long? = null,
)

object ZmanimCalculator {

    // ── Main entry — builds calendar once, calculates everything ─────────────

    fun calculateAll(settings: AppSettings): DailyCalculations {
        return try {
            val tz = TimeZone.getTimeZone(settings.city.timeZoneId)
            val geo = GeoLocation(
                settings.city.nameEnglish,
                settings.city.latitude,
                settings.city.longitude,
                settings.city.elevation,
                tz,
            )
            val cal = ComplexZmanimCalendar(geo)
            settings.testDateTime?.let { testMs ->
                cal.calendar = Calendar.getInstance(tz).apply { timeInMillis = testMs }
            }
            // candleLightingOffset not used — we compute zmaniyot minutes manually

            val erevCal    = calAdvancedBy(cal, tz, daysToNextErev(cal))
            val shabbatCal = calAdvancedBy(cal, tz, daysToNextShabbatOrYomTov(cal))
            val tzaitMs = tzaitZmaniyot(cal)?.time
            val zmanim = buildZmanim(cal, erevCal, shabbatCal, tz, settings.prayerSystem)
            val (weekday, shabbat) = buildPrayerTimes(cal, erevCal, shabbatCal, tz, settings)
            val baseHebrewInfo = getHebrewCalendarInfo(settings.prayerSystem, settings.testDateTime, tzaitMs)
            val nowMs = settings.testDateTime ?: System.currentTimeMillis()
            val israelTz = TimeZone.getTimeZone("Asia/Jerusalem")
            val jewishCalForFast = JewishCalendar(Calendar.getInstance(israelTz).apply { timeInMillis = nowMs }).also {
                if (tzaitMs != null && nowMs > tzaitMs) it.forward(Calendar.DATE, 1)
                it.inIsrael = true
            }
            val fastData = findUpcomingFast(jewishCalForFast, cal, tz, settings.prayerSystem)
            // When a special Shabbat label exists and the fast is still in the future,
            // suppress the advance fast notice so the special Shabbat shows instead.
            // On the fast day itself (daysAhead == 0) the fast always wins.
            val suppressFast = fastData != null
                && fastData.daysAhead > 0
                && baseHebrewInfo.holidayLabel.isNotEmpty()
            val hebrewInfo = baseHebrewInfo.copy(
                fastName  = if (suppressFast) "" else fastData?.name  ?: "",
                fastStart = if (suppressFast) "" else fastData?.start ?: "",
                fastEnd   = if (suppressFast) "" else fastData?.end   ?: "",
            )

            DailyCalculations(zmanim, weekday, shabbat, hebrewInfo, tzaitMs)
        } catch (e: Exception) {
            DailyCalculations()
        }
    }

    // ── Zmanim ───────────────────────────────────────────────────────────────

    private fun buildZmanim(
        cal: ComplexZmanimCalendar,
        erevCal: ComplexZmanimCalendar,
        shabbatCal: ComplexZmanimCalendar,
        tz: TimeZone,
        system: PrayerSystem,
    ): List<ZmanItem> {
        val tzait18 = tzaitZmaniyot(cal)
        return when (system) {
            PrayerSystem.MIZRACHI -> listOf(
                ZmanItem("עלות השחר",         fmt(cal.alos72Zmanis, tz)),
                ZmanItem("זמן ציצית ותפילין", fmt(cal.misheyakir11Point5Degrees, tz)),
                ZmanItem("הנץ החמה",           fmt(cal.sunrise, tz)),
                ZmanItem("סו\"זק\"ש מג\"א",   fmt(cal.sofZmanShmaMGA, tz)),
                ZmanItem("חצות",              fmt(cal.chatzos, tz)),
                ZmanItem("שקיעה",             fmt(cal.sunset, tz)),
                ZmanItem("צאת הכוכבים",       fmt(tzait18, tz)),
                ZmanItem("הדלקת נרות",        fmt(candleLightingZmaniyot(erevCal, system), tz)),
                ZmanItem("צאת השבת",          fmt(shabbatCal.tzaisGeonim8Point5Degrees, tz)),
            )
            PrayerSystem.ASHKENAZ -> listOf(
                ZmanItem("עלות השחר",         fmt(cal.alos90, tz)),
                ZmanItem("זמן ציצית ותפילין", fmt(cal.misheyakir11Point5Degrees, tz)),
                ZmanItem("הנץ החמה",           fmt(cal.sunrise, tz)),
                ZmanItem("סו\"זק\"ש גר\"א",   fmt(cal.sofZmanShmaGRA, tz)),
                ZmanItem("חצות",              fmt(cal.chatzos, tz)),
                ZmanItem("שקיעה",             fmt(cal.sunset, tz)),
                ZmanItem("צאת הכוכבים",       fmt(tzait18, tz)),
                ZmanItem("הדלקת נרות",        fmt(candleLightingZmaniyot(erevCal, system), tz)),
                ZmanItem("צאת השבת",          fmt(shabbatCal.tzais72, tz)),
            )
        }
    }

    // ── Prayer times ─────────────────────────────────────────────────────────

    private fun buildPrayerTimes(
        cal: ComplexZmanimCalendar,
        erevCal: ComplexZmanimCalendar,
        shabbatCal: ComplexZmanimCalendar,
        tz: TimeZone,
        settings: AppSettings,
    ): Pair<List<TefilaItem>, List<TefilaItem>> {
        val shacharitWeekday = when (settings.shacharitWeekdayMode) {
            PrayerTimeMode.MANUAL -> settings.shacharitWeekdayTime
            PrayerTimeMode.HANETZ -> fmt(cal.sunrise?.let { Date(it.time + settings.shacharitWeekdayOffset * 60_000L) }, tz)
        }
        val minchaWeekday = cal.sunset?.let { Date(it.time + settings.minchaWeekdayOffset * 60_000L) }
        val arvitWeekday  = tzaitZmaniyot(cal)

        val weekday = listOf(
            TefilaItem("שחרית", shacharitWeekday),
            TefilaItem("מנחה",  fmt(minchaWeekday, tz)),
            TefilaItem("ערבית", fmt(arvitWeekday,  tz)),
        )

        val shacharitShabbat = when (settings.shacharitShabbatMode) {
            PrayerTimeMode.MANUAL -> settings.shacharitShabbatTime
            PrayerTimeMode.HANETZ -> fmt(shabbatCal.sunrise?.let { Date(it.time + settings.shacharitShabbatOffset * 60_000L) }, tz)
        }
        val candleLighting  = candleLightingZmaniyot(erevCal, settings.prayerSystem)
        val kabbalatShabbat = candleLighting?.let { Date(it.time + settings.kabbalatShabbatOffset * 60_000L) }
        val minchaShabbat   = candleLighting?.let { Date(it.time + settings.minchaShabbatOffset   * 60_000L) }
        val arvitHavdalah   = tzaitZmaniyot(shabbatCal)?.let { Date(it.time + settings.arvitShabbatOffset * 60_000L) }

        val shabbat = listOf(
            TefilaItem("מנחה וקבלת שבת", fmt(kabbalatShabbat, tz)),
            TefilaItem("שחרית",          shacharitShabbat),
            TefilaItem("מנחה",           fmt(minchaShabbat, tz)),
            TefilaItem("ערבית והבדלה",   fmt(arvitHavdalah,  tz)),
        )

        return Pair(weekday, shabbat)
    }

    // ── Hebrew calendar info ──────────────────────────────────────────────────

    fun getHebrewCalendarInfo(system: PrayerSystem = PrayerSystem.MIZRACHI, testDateTimeMs: Long? = null, tzaitMs: Long? = null): HebrewCalendarInfo {
        return try {
            val nowMs = testDateTimeMs ?: System.currentTimeMillis()
            val israelTz = TimeZone.getTimeZone("Asia/Jerusalem")
            val jewishCal = JewishCalendar(Calendar.getInstance(israelTz).apply { timeInMillis = nowMs })
            jewishCal.inIsrael = true
            // Hebrew day starts at nightfall — advance if current time is past today's Tzait
            if (tzaitMs != null && nowMs > tzaitMs) {
                jewishCal.forward(Calendar.DATE, 1)
            }
            val formatter = HebrewDateFormatter().apply { isHebrewFormat = true }

            val dateStr = formatter.format(jewishCal)
            val dayOfWeek = when (jewishCal.dayOfWeek) {
                Calendar.SUNDAY    -> "יום ראשון"
                Calendar.MONDAY    -> "יום שני"
                Calendar.TUESDAY   -> "יום שלישי"
                Calendar.WEDNESDAY -> "יום רביעי"
                Calendar.THURSDAY  -> "יום חמישי"
                Calendar.FRIDAY    -> "יום שישי"
                Calendar.SATURDAY  -> "שבת קודש"
                else               -> ""
            }

            // Parasha: advance to next Shabbat (Israel cycle)
            val shabbatCal = jewishCal.clone() as JewishCalendar
            shabbatCal.inIsrael = true
            while (shabbatCal.dayOfWeek != Calendar.SATURDAY) {
                shabbatCal.forward(Calendar.DATE, 1)
            }

            // Show holiday name (no haftara) from today through the last day of any holiday this week
            val holidayScan = jewishCal.clone() as JewishCalendar
            holidayScan.inIsrael = true
            var foundHoliday: JewishCalendar? = null
            while (holidayScan.absDate <= shabbatCal.absDate) {
                if (holidayScan.isYomTovAssurBemelacha || holidayScan.isCholHamoed) {
                    foundHoliday = holidayScan.clone() as JewishCalendar
                    break
                }
                holidayScan.forward(Calendar.DATE, 1)
            }

            val parasha: String
            val haftara: String
            if (foundHoliday != null) {
                parasha = formatter.formatYomTov(foundHoliday)
                haftara = ""
            } else {
                parasha = formatter.formatParsha(shabbatCal)
                haftara = haftaraForParsha(shabbatCal)
            }

            // Daf Yomi
            val daf = jewishCal.dafYomiBavli
            val dafStr = if (daf != null) formatter.formatDafYomiBavli(daf) else ""

            val month = jewishCal.jewishMonth
            val day = jewishCal.jewishDayOfMonth

            // Shabbat Mevorchim week: true Sun→Sat when this week's Shabbat is Shabbat Mevorchim
            val isMevorchimWeek = isShabbatMevorchimWeek(jewishCal)
            val moladStr = if (isMevorchimWeek) getMoladString(jewishCal, formatter) else ""

            // Omer count
            val omerDay = jewishCal.dayOfOmer
            val omerStr = if (omerDay > 0) formatOmer(omerDay, system) else ""

            // תחנון / צדקתך
            val isShabbat = jewishCal.dayOfWeek == Calendar.SATURDAY
            val noTachanunPeriod = isTachanunRules(jewishCal)
            val tachanunOmitted = !isShabbat && noTachanunPeriod
            val tzidkatchaOmitted = isShabbat && noTachanunPeriod

            // שם החג / יום מיוחד
            val holidayLabel = getHolidayLabel(jewishCal)
            val alHaNisim = holidayLabel == "חנוכה" || holidayLabel == "פורים" || holidayLabel == "שושן פורים"

            HebrewCalendarInfo(
                hebrewDate = dateStr,
                dayOfWeek = dayOfWeek,
                parasha = parasha,
                haftara = haftara,
                dafYomi = dafStr,
                isYaaleVeyavo = isYaaleVeyavDay(jewishCal),
                isMashivHaruach = isMashivHaruach(month, day),
                isShabbatMevorchim = isMevorchimWeek,
                isWinterBlessingSeason = isWinterBlessingSeason(month, day),
                moladText = moladStr,
                omerText = omerStr,
                isTachanunOmitted = tachanunOmitted,
                isTzidkatchaOmitted = tzidkatchaOmitted,
                fastName = "",
                isAlHaNisim = alHaNisim,
                holidayLabel = holidayLabel,
            )
        } catch (e: Exception) {
            HebrewCalendarInfo()
        }
    }

    // ── Seasonal helpers ──────────────────────────────────────────────────────

    private fun isMashivHaruach(month: Int, day: Int): Boolean = when (month) {
        JewishCalendar.TISHREI -> day >= 22
        JewishCalendar.CHESHVAN, JewishCalendar.KISLEV, JewishCalendar.TEVES,
        JewishCalendar.SHEVAT, JewishCalendar.ADAR, JewishCalendar.ADAR_II -> true
        JewishCalendar.NISSAN -> day < 15
        else -> false
    }

    private fun isWinterBlessingSeason(month: Int, day: Int): Boolean = when (month) {
        JewishCalendar.CHESHVAN -> day >= 7
        JewishCalendar.KISLEV, JewishCalendar.TEVES, JewishCalendar.SHEVAT,
        JewishCalendar.ADAR, JewishCalendar.ADAR_II -> true
        JewishCalendar.NISSAN -> day < 15
        else -> false
    }

    // Core no-Tachanun rules — shared for both תחנון (weekday) and צדקתך (Shabbat)
    private fun isTachanunRules(cal: JewishCalendar): Boolean {
        val month = cal.jewishMonth
        val day   = cal.jewishDayOfMonth

        if (cal.isRoshChodesh) return true

        // חנוכה — KosherJava handles variable Kislev length correctly
        val chanukah = try { cal.isChanukah } catch (e: Exception) {
            (month == JewishCalendar.KISLEV && day >= 25) || (month == JewishCalendar.TEVES && day <= 3)
        }
        if (chanukah) return true

        when (month) {
            JewishCalendar.SHEVAT  -> if (day == 15) return true                   // ט"ו בשבט
            JewishCalendar.ADAR    -> {
                // In a leap year ADAR = Adar I; 14 = Purim Katan
                val isLeap = cal.isJewishLeapYear
                if (isLeap  && day == 14) return true
                if (!isLeap && day in 14..15) return true                          // פורים / שושן פורים
            }
            JewishCalendar.ADAR_II -> if (day in 14..15) return true              // פורים בשנה מעוברת
            JewishCalendar.NISSAN  -> return true                                  // כל ניסן
            JewishCalendar.IYAR    -> {
                if (day == 5)  return true                                         // יום העצמאות
                if (day == 18) return true                                         // ל"ג בעומר
                if (day == 28) return true                                         // יום ירושלים
            }
            JewishCalendar.SIVAN   -> if (day <= 12) return true                  // שבועות + ימי איסרו חג
            JewishCalendar.AV      -> if (day == 9 || day == 15) return true      // תשעה באב + ט"ו באב
            JewishCalendar.ELUL    -> if (day == 29) return true                  // ערב ראש השנה
            JewishCalendar.TISHREI -> if (day <= 23) return true                  // א' תשרי–כ"ג (ר"ה–אסרו חג סוכות)
        }
        return false
    }

    // ── Fast detection + time calculation ────────────────────────────────────

    private data class FastInfo(val name: String, val start: String, val end: String, val daysAhead: Int = 0)

    // Scan from today through the next 7 days — covers "display from Shabbat before the fast"
    private fun findUpcomingFast(
        todayCal: JewishCalendar,
        mainCal: ComplexZmanimCalendar,
        tz: TimeZone,
        system: PrayerSystem,
    ): FastInfo? {
        val scanCal = todayCal.clone() as JewishCalendar
        for (daysAhead in 0..6) {
            val name = getFastName(scanCal)
            if (name.isNotEmpty()) {
                val fastZmanim = mainCal.clone() as ComplexZmanimCalendar
                fastZmanim.calendar = Calendar.getInstance(tz).apply {
                    timeInMillis = mainCal.calendar.timeInMillis
                    add(Calendar.DATE, daysAhead)
                }
                // Tisha BeAv starts at sunset of the previous day
                val isTishaBeAv = scanCal.jewishMonth == JewishCalendar.AV && scanCal.jewishDayOfMonth in 9..10
                val start = if (isTishaBeAv) {
                    val prevZmanim = mainCal.clone() as ComplexZmanimCalendar
                    prevZmanim.calendar = Calendar.getInstance(tz).apply {
                        timeInMillis = mainCal.calendar.timeInMillis
                        add(Calendar.DATE, daysAhead - 1)
                    }
                    fmt(prevZmanim.sunset, tz)
                } else {
                    when (system) {
                        PrayerSystem.MIZRACHI -> fmt(fastZmanim.alos72Zmanis, tz)
                        PrayerSystem.ASHKENAZ -> fmt(fastZmanim.alos90, tz)
                    }
                }
                val end = fmt(tzaitZmaniyot(fastZmanim), tz)
                return FastInfo(name, start, end, daysAhead)
            }
            scanCal.forward(Calendar.DATE, 1)
        }
        return null
    }

    // Fast day name — empty string on non-fast days; Yom Kippur excluded (shows via parasha/holiday display)
    private fun getFastName(cal: JewishCalendar): String {
        if (!cal.isTaanis) return ""
        val m = cal.jewishMonth
        val d = cal.jewishDayOfMonth
        return when {
            m == JewishCalendar.TISHREI && d in 3..4   -> "גדליה"
            m == JewishCalendar.TEVES   && d == 10     -> "עשרה בטבת"
            (m == JewishCalendar.ADAR || m == JewishCalendar.ADAR_II) && d in 11..13 -> "אסתר"
            m == JewishCalendar.TAMMUZ  && d in 17..18 -> "שבעה עשר בתמוז"
            m == JewishCalendar.AV      && d in 9..10  -> "תשעה באב"
            else -> ""
        }
    }

    // שם החג/היום המיוחד לתצוגה בלוח התפילות — ריק אם אין
    private fun getHolidayLabel(cal: JewishCalendar): String {
        val m = cal.jewishMonth
        val d = cal.jewishDayOfMonth

        // חנוכה
        val isChanukah = try { cal.isChanukah } catch (e: Exception) {
            (m == JewishCalendar.KISLEV && d >= 25) || (m == JewishCalendar.TEVES && d <= 3)
        }
        if (isChanukah) return "חנוכה"

        // ט"ו בשבט
        if (m == JewishCalendar.SHEVAT && d == 15) return "ט\"ו בשבט"

        // פורים ושושן פורים
        val isLeap = cal.isJewishLeapYear
        if (!isLeap && m == JewishCalendar.ADAR) {
            if (d == 14) return "פורים"
            if (d == 15) return "שושן פורים"
        }
        if (isLeap && m == JewishCalendar.ADAR_II) {
            if (d == 14) return "פורים"
            if (d == 15) return "שושן פורים"
        }

        // ימים לאומיים — KosherJava מטפל בהזזות דרך yomTovIndex
        val modernCal = (cal.clone() as JewishCalendar).also { it.isUseModernHolidays = true }
        when (modernCal.yomTovIndex) {
            JewishCalendar.YOM_HASHOAH      -> return "יום השואה והגבורה"
            JewishCalendar.YOM_HAZIKARON    -> return "יום הזכרון"
            JewishCalendar.YOM_HAATZMAUT    -> return "יום העצמאות"
            JewishCalendar.YOM_YERUSHALAYIM -> return "יום ירושלים"
        }

        // ניסן/אייר/אב — ימים קבועים
        if (m == JewishCalendar.IYAR && d == 14) return "פסח שני"
        if (m == JewishCalendar.IYAR && d == 18) return "ל\"ג בעומר"
        if (m == JewishCalendar.AV   && d == 15) return "ט\"ו באב"

        // שבתות מיוחדות — מוצג כל השבוע (ראשון עד שבת כולל)
        return getSpecialShabbatLabel(cal)
    }

    // שם השבת המיוחדת של השבוע הנוכחי — מוצא את השבת הקרובה (כולל היום אם שבת)
    private fun getSpecialShabbatLabel(cal: JewishCalendar): String {
        val shabbatCal = cal.clone() as JewishCalendar
        shabbatCal.inIsrael = true
        while (shabbatCal.dayOfWeek != Calendar.SATURDAY) {
            shabbatCal.forward(Calendar.DATE, 1)
        }

        // ארבע פרשיות
        when (shabbatCal.specialShabbos) {
            JewishCalendar.Parsha.SHKALIM    -> return "שבת שקלים"
            JewishCalendar.Parsha.ZACHOR     -> return "שבת זכור"
            JewishCalendar.Parsha.PARA       -> return "שבת פרה"
            JewishCalendar.Parsha.HACHODESH  -> return "שבת החודש"
            else -> Unit
        }

        // שבת הגדול: שבת שלפני פסח (י-י"ד ניסן)
        val sm = shabbatCal.jewishMonth
        val sd = shabbatCal.jewishDayOfMonth
        if (sm == JewishCalendar.NISSAN && sd in 10..14) return "שבת הגדול"

        // שבת חזון (שבת שלפני תשעה באב): ג'–ח' אב  |  שבת נחמו (שבת שאחרי): י'–ט"ז אב
        if (sm == JewishCalendar.AV && sd in 3..8)   return "שבת חזון"
        if (sm == JewishCalendar.AV && sd in 10..16) return "שבת נחמו"

        // שבת שירה — לפי הפרשה (בשלח)
        return when (shabbatCal.parshah) {
            JewishCalendar.Parsha.BESHALACH -> "שבת שירה"
            else -> ""
        }
    }

    // יעלה ויבוא: ראש חודש + כל ימי פסח, סוכות ושמיני עצרת, שבועות
    // (ראש השנה ויום כיפור — לא נאמר יעלה ויבוא בהם)
    private fun isYaaleVeyavDay(cal: JewishCalendar): Boolean {
        if (cal.isRoshChodesh) return true
        val month = cal.jewishMonth
        val day = cal.jewishDayOfMonth
        if (month == JewishCalendar.NISSAN  && day in 15..21) return true  // כל ימי פסח (ישראל)
        if (month == JewishCalendar.TISHREI && day in 15..22) return true  // סוכות + שמיני עצרת/שמחת תורה
        if (month == JewishCalendar.SIVAN   && day == 6)      return true  // שבועות (ישראל)
        return false
    }

    // True on every day of the week whose Shabbat is Shabbat Mevorchim (Sun–Sat).
    // Skips Elul: Tishrei/Rosh Hashana has no Shabbat Mevorchim announcement.
    private fun isShabbatMevorchimWeek(cal: JewishCalendar): Boolean {
        if (cal.jewishMonth == JewishCalendar.ELUL) return false
        // How many days until (or 0 if already) Shabbat this week
        val daysToShabbat = (7 - cal.dayOfWeek) % 7
        val shabbatCal = cal.clone() as JewishCalendar
        if (daysToShabbat > 0) shabbatCal.forward(Calendar.DATE, daysToShabbat)
        return shabbatCal.isShabbosMevorchim
    }

    // ── Molad ─────────────────────────────────────────────────────────────────

    private fun getMoladString(cal: JewishCalendar, formatter: HebrewDateFormatter): String {
        return try {
            val next = cal.clone() as JewishCalendar
            next.setJewishDate(cal.jewishYear, cal.jewishMonth, cal.daysInJewishMonth)
            next.forward(Calendar.DATE, 1)
            val tz = TimeZone.getTimeZone("Asia/Jerusalem")
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
            val moladDate = next.moladAsDate
            val moladCal = Calendar.getInstance(tz).apply { time = moladDate }
            val dayName = when (moladCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY    -> "ראשון"
                Calendar.MONDAY    -> "שני"
                Calendar.TUESDAY   -> "שלישי"
                Calendar.WEDNESDAY -> "רביעי"
                Calendar.THURSDAY  -> "חמישי"
                Calendar.FRIDAY    -> "שישי"
                Calendar.SATURDAY  -> "שבת"
                else               -> ""
            }
            "מולד חודש ${formatter.formatMonth(next)}: יום $dayName ${timeFmt.format(moladDate)}"
        } catch (e: Exception) {
            ""
        }
    }

    // ── Omer count ────────────────────────────────────────────────────────────

    private fun formatOmer(day: Int, system: PrayerSystem): String {
        if (day <= 0 || day > 49) return ""
        val dayStr = omerDays[day] ?: return ""
        val weeks = day / 7
        val remainder = day % 7

        // No full week yet — identical in both traditions
        if (weeks == 0) return "היום $dayStr לעומר"

        val weekPart = "שהם ${omerWeeks[weeks]}"
        val remPart = if (remainder > 0) " ${omerRemainders[remainder]}" else ""

        return when (system) {
            // מזרחי: היום X ימים לעומר שהם Y שבועות וZ ימים
            PrayerSystem.MIZRACHI -> "היום $dayStr לעומר $weekPart$remPart"
            // אשכנז: היום X ימים שהם Y שבועות וZ ימים לעומר
            PrayerSystem.ASHKENAZ -> "היום $dayStr $weekPart$remPart לעומר"
        }
    }

    private val omerDays = mapOf(
        1 to "יום אחד",        2 to "שני ימים",       3 to "שלשה ימים",
        4 to "ארבעה ימים",     5 to "חמישה ימים",      6 to "ששה ימים",
        7 to "שבעה ימים",      8 to "שמונה ימים",      9 to "תשעה ימים",
        10 to "עשרה ימים",     11 to "אחד עשר יום",    12 to "שנים עשר יום",
        13 to "שלשה עשר יום",  14 to "ארבעה עשר יום",  15 to "חמישה עשר יום",
        16 to "ששה עשר יום",   17 to "שבעה עשר יום",   18 to "שמונה עשר יום",
        19 to "תשעה עשר יום",  20 to "עשרים יום",
        21 to "עשרים ואחד יום",    22 to "עשרים ושניים יום",  23 to "עשרים ושלשה יום",
        24 to "עשרים וארבעה יום",  25 to "עשרים וחמישה יום",  26 to "עשרים וששה יום",
        27 to "עשרים ושבעה יום",   28 to "עשרים ושמונה יום",  29 to "עשרים ותשעה יום",
        30 to "שלשים יום",
        31 to "שלשים ואחד יום",    32 to "שלשים ושניים יום",  33 to "שלשים ושלשה יום",
        34 to "שלשים וארבעה יום",  35 to "שלשים וחמישה יום",  36 to "שלשים וששה יום",
        37 to "שלשים ושבעה יום",   38 to "שלשים ושמונה יום",  39 to "שלשים ותשעה יום",
        40 to "ארבעים יום",
        41 to "ארבעים ואחד יום",   42 to "ארבעים ושניים יום", 43 to "ארבעים ושלשה יום",
        44 to "ארבעים וארבעה יום", 45 to "ארבעים וחמישה יום", 46 to "ארבעים וששה יום",
        47 to "ארבעים ושבעה יום",  48 to "ארבעים ושמונה יום", 49 to "ארבעים ותשעה יום",
    )

    private val omerWeeks = mapOf(
        1 to "שבוע אחד",      2 to "שני שבועות",
        3 to "שלשה שבועות",   4 to "ארבעה שבועות",
        5 to "חמישה שבועות",  6 to "ששה שבועות",
        7 to "שבעה שבועות",
    )

    private val omerRemainders = mapOf(
        1 to "ויום אחד",    2 to "ושני ימים",
        3 to "ושלשה ימים",  4 to "וארבעה ימים",
        5 to "וחמישה ימים", 6 to "וששה ימים",
    )

    // ── Haftara lookup ────────────────────────────────────────────────────────

    private fun haftaraForParsha(shabbatCal: JewishCalendar): String {
        // Special Shabbatot override the regular haftara
        val special = shabbatCal.specialShabbos
        if (special != JewishCalendar.Parsha.NONE) {
            return when (special) {
                JewishCalendar.Parsha.SHKALIM   -> "בשנת שבע"
                JewishCalendar.Parsha.ZACHOR     -> "ויאמר שמואל"
                JewishCalendar.Parsha.PARA       -> "ויהי דבר"
                JewishCalendar.Parsha.HACHODESH  -> "כה אמר"
                else -> ""
            }
        }
        // Shabbat Hagadol (Shabbat before Pesach: 10–14 Nisan)
        if (shabbatCal.jewishMonth == JewishCalendar.NISSAN &&
            shabbatCal.jewishDayOfMonth in 10..14) {
            return "וערבה לה'"
        }
        return when (shabbatCal.parshah) {
            JewishCalendar.Parsha.BERESHIS             -> "כה אמר"
            JewishCalendar.Parsha.NOACH                -> "רני עקרה"
            JewishCalendar.Parsha.LECH_LECHA           -> "למה תאמר"
            JewishCalendar.Parsha.VAYERA               -> "ואשה אחת"
            JewishCalendar.Parsha.CHAYEI_SARA          -> "והמלך דוד"
            JewishCalendar.Parsha.TOLDOS               -> "משא דבר"
            JewishCalendar.Parsha.VAYETZEI             -> "ויברח יעקב"
            JewishCalendar.Parsha.VAYISHLACH           -> "ועמי תלואים"
            JewishCalendar.Parsha.VAYESHEV             -> "כה אמר"
            JewishCalendar.Parsha.MIKETZ               -> "וייקץ שלמה"
            JewishCalendar.Parsha.VAYIGASH             -> "ויהי דבר"
            JewishCalendar.Parsha.VAYECHI              -> "ויקרבו ימי"
            JewishCalendar.Parsha.SHEMOS               -> "דברי ירמיהו"
            JewishCalendar.Parsha.VAERA                -> "כה אמר"
            JewishCalendar.Parsha.BO                   -> "הדבר אשר"
            JewishCalendar.Parsha.BESHALACH            -> "ודבורה אשה"
            JewishCalendar.Parsha.YISRO                -> "בשנת מות"
            JewishCalendar.Parsha.MISHPATIM            -> "הדבר אשר"
            JewishCalendar.Parsha.TERUMAH              -> "וה' נתן"
            JewishCalendar.Parsha.TETZAVEH             -> "אתה בן"
            JewishCalendar.Parsha.KI_SISA              -> "ויהי ימים"
            JewishCalendar.Parsha.VAYAKHEL             -> "ויעש חירם"
            JewishCalendar.Parsha.PEKUDEI              -> "ותשלם כל"
            JewishCalendar.Parsha.VAYAKHEL_PEKUDEI     -> "ויעש חירם"
            JewishCalendar.Parsha.VAYIKRA              -> "עם זו"
            JewishCalendar.Parsha.TZAV                 -> "כה אמר"
            JewishCalendar.Parsha.SHMINI               -> "ויסף עוד"
            JewishCalendar.Parsha.TAZRIA               -> "ואיש בא"
            JewishCalendar.Parsha.METZORA              -> "וארבעה אנשים"
            JewishCalendar.Parsha.TAZRIA_METZORA       -> "וארבעה אנשים"
            JewishCalendar.Parsha.ACHREI_MOS           -> "ויהי דבר"
            JewishCalendar.Parsha.KEDOSHIM             -> "הלא כבני"
            JewishCalendar.Parsha.ACHREI_MOS_KEDOSHIM  -> "הלא כבני"
            JewishCalendar.Parsha.EMOR                 -> "והכהנים הלוים"
            JewishCalendar.Parsha.BEHAR                -> "ויהי דבר"
            JewishCalendar.Parsha.BECHUKOSAI           -> "ה' עוזי"
            JewishCalendar.Parsha.BEHAR_BECHUKOSAI     -> "ה' עוזי"
            JewishCalendar.Parsha.BAMIDBAR             -> "והיה מספר"
            JewishCalendar.Parsha.NASSO                -> "ויהי איש"
            JewishCalendar.Parsha.BEHAALOSCHA          -> "רני ושמחי"
            JewishCalendar.Parsha.SHLACH               -> "וישלח יהושע"
            JewishCalendar.Parsha.KORACH               -> "ויאמר שמואל"
            JewishCalendar.Parsha.CHUKAS               -> "ויפתח הגלעדי"
            JewishCalendar.Parsha.BALAK                -> "והיה שארית"
            JewishCalendar.Parsha.CHUKAS_BALAK         -> "והיה שארית"
            JewishCalendar.Parsha.PINCHAS              -> "ויהי יד"
            JewishCalendar.Parsha.MATOS                -> "דברי ירמיהו"
            JewishCalendar.Parsha.MASEI                -> "שמעו דבר"
            JewishCalendar.Parsha.MATOS_MASEI          -> "שמעו דבר"
            JewishCalendar.Parsha.DEVARIM              -> "חזון ישעיהו"
            JewishCalendar.Parsha.VAESCHANAN           -> "נחמו נחמו"
            JewishCalendar.Parsha.EIKEV                -> "ותאמר ציון"
            JewishCalendar.Parsha.REEH                 -> "עניה סוערה"
            JewishCalendar.Parsha.SHOFTIM              -> "אנכי אנכי"
            JewishCalendar.Parsha.KI_SEITZEI           -> "רני עקרה"
            JewishCalendar.Parsha.KI_SAVO              -> "קומי אורי"
            JewishCalendar.Parsha.NITZAVIM             -> "שוש אשיש"
            JewishCalendar.Parsha.VAYEILECH            -> "שובה ישראל"
            JewishCalendar.Parsha.NITZAVIM_VAYEILECH   -> "שוש אשיש"
            JewishCalendar.Parsha.HAAZINU              -> "וידבר דוד"
            JewishCalendar.Parsha.VZOS_HABERACHA       -> "ויהי אחרי"
            else -> ""
        }
    }

    // ── Shabbat / Yom Tov calendar helpers ───────────────────────────────────

    private fun calAdvancedBy(base: ComplexZmanimCalendar, tz: TimeZone, days: Int): ComplexZmanimCalendar {
        if (days == 0) return base
        return (base.clone() as ComplexZmanimCalendar).also { c ->
            c.calendar = Calendar.getInstance(tz).apply {
                timeInMillis = base.calendar.timeInMillis
                add(Calendar.DATE, days)
            }
        }
    }

    // Days until the next Erev Shabbat or Erev Yom Tov (0 = today is already Erev).
    private fun daysToNextErev(base: ComplexZmanimCalendar): Int {
        val israelTz = TimeZone.getTimeZone("Asia/Jerusalem")
        val scan = JewishCalendar(Calendar.getInstance(israelTz).apply { timeInMillis = base.calendar.timeInMillis })
        scan.inIsrael = true
        for (i in 0..8) {
            if (scan.dayOfWeek == Calendar.FRIDAY) return i
            val tomorrow = (scan.clone() as JewishCalendar).also { it.forward(Calendar.DATE, 1) }
            if (tomorrow.isYomTovAssurBemelacha) return i
            scan.forward(Calendar.DATE, 1)
        }
        return (Calendar.FRIDAY - base.calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
    }

    // Days until the next Shabbat or Yom Tov (0 = today is already Shabbat/YT).
    private fun daysToNextShabbatOrYomTov(base: ComplexZmanimCalendar): Int {
        val israelTz = TimeZone.getTimeZone("Asia/Jerusalem")
        val scan = JewishCalendar(Calendar.getInstance(israelTz).apply { timeInMillis = base.calendar.timeInMillis })
        scan.inIsrael = true
        for (i in 0..8) {
            if (scan.dayOfWeek == Calendar.SATURDAY || scan.isYomTovAssurBemelacha) return i
            scan.forward(Calendar.DATE, 1)
        }
        return (Calendar.SATURDAY - base.calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
    }

    // 20 (or 18) zmaniyot minutes (MGA sha'ah) before sunset
    private fun candleLightingZmaniyot(erevCal: ComplexZmanimCalendar, system: PrayerSystem): Date? {
        val offsetMin = if (system == PrayerSystem.MIZRACHI) 20 else 18
        val shaahMs = erevCal.shaahZmanisMGA.takeIf { it > 0 } ?: return null
        val sunset = erevCal.sunset ?: return null
        return Date(sunset.time - offsetMin * shaahMs / 60)
    }

    // 18 zmaniyot minutes (MGA sha'ah) after sunset
    private fun tzaitZmaniyot(cal: ComplexZmanimCalendar): Date? {
        val shaahMs = cal.shaahZmanisMGA.takeIf { it > 0 } ?: return null
        val sunset = cal.sunset ?: return null
        return Date(sunset.time + 18 * shaahMs / 60)
    }

    // ── Format helper ─────────────────────────────────────────────────────────

    private fun fmt(date: Date?, tz: TimeZone): String {
        if (date == null) return "--:--"
        return SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }.format(date)
    }
}
