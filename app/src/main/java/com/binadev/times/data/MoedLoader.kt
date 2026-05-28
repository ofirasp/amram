package com.binadev.times.data

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.util.Calendar
import java.util.Date

object MoedLoader {

    fun load(context: Context, testDateTimeMs: Long? = null, tzaitMs: Long? = null): List<ContentSlide> {
        val nowMs = testDateTimeMs ?: System.currentTimeMillis()
        val jewishCal = JewishCalendar(Date(nowMs))
        if (tzaitMs != null && nowMs > tzaitMs) jewishCal.forward(Calendar.DATE, 1)
        val today: JewishDate = jewishCal
        val todayAbs = today.absDate
        val year = today.jewishYear

        return try {
            context.assets.open("amram/moedtext2.csv").bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readLines()
                    .drop(1)
                    .filter { it.isNotBlank() }
                    .flatMap { line ->
                        val cols = line.split("\t")
                        if (cols.size < 7) return@flatMap emptyList()

                        val toDateStr   = cols[0].trim()
                        val fromDateStr = cols[1].trim()
                        val imageAsset  = cols[2].trim().takeIf { it.isNotBlank() }
                        val content3    = cols[3].trim()
                        val content2    = cols[4].trim()
                        val content1    = cols[5].trim()
                        val title       = cols[6].trim()

                        val fromAbs = parseHebrewDate(fromDateStr, year)?.absDate ?: return@flatMap emptyList()
                        val toAbs   = parseHebrewDate(toDateStr,   year)?.absDate ?: return@flatMap emptyList()

                        if (todayAbs < fromAbs || todayAbs > toAbs) return@flatMap emptyList()

                        buildList {
                            if (content1.isNotBlank()) add(ContentSlide(title, SlideContent.TorahLesson(content1, imageAsset)))
                            if (content2.isNotBlank()) add(ContentSlide(title, SlideContent.TorahLesson(content2, imageAsset)))
                            if (content3.isNotBlank()) add(ContentSlide(title, SlideContent.TorahLesson(content3, imageAsset)))
                        }
                    }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseHebrewDate(dateStr: String, year: Int): JewishDate? {
        val spaceIdx = dateStr.indexOf(' ')
        if (spaceIdx < 0) return null
        val dayStr   = dateStr.substring(0, spaceIdx)
        val monthStr = dateStr.substring(spaceIdx + 1).trim()
        val day      = parseHebrewNumber(dayStr) ?: return null
        val month    = parseHebrewMonth(monthStr) ?: return null
        return try { JewishDate(year, month, day) } catch (e: Exception) { null }
    }

    private fun parseHebrewNumber(s: String): Int? {
        // Remove ASCII quotes and Hebrew geresh/gershayim punctuation marks
        val clean = s
            .replace("\"", "")
            .replace("'", "")
            .replace("׳", "")
            .replace("״", "")
        var sum = 0
        for (ch in clean) {
            sum += hebrewLetterValue(ch) ?: return null
        }
        return if (sum > 0) sum else null
    }

    private fun hebrewLetterValue(c: Char): Int? = when (c) {
        'א' -> 1
        'ב' -> 2
        'ג' -> 3
        'ד' -> 4
        'ה' -> 5
        'ו' -> 6
        'ז' -> 7
        'ח' -> 8
        'ט' -> 9
        'י' -> 10
        'כ', 'ך' -> 20
        'ל' -> 30
        'מ', 'ם' -> 40
        'נ', 'ן' -> 50
        'ס' -> 60
        'ע' -> 70
        'פ', 'ף' -> 80
        'צ', 'ץ' -> 90
        'ק' -> 100
        'ר' -> 200
        'ש' -> 300
        'ת' -> 400
        else -> null
    }

    private fun parseHebrewMonth(s: String): Int? = when (s) {
        "ניסן"            -> JewishDate.NISSAN
        "אייר"            -> JewishDate.IYAR
        "סיון"            -> JewishDate.SIVAN
        "תמוז"            -> JewishDate.TAMMUZ
        "אב"              -> JewishDate.AV
        "אלול"            -> JewishDate.ELUL
        "תשרי"            -> JewishDate.TISHREI
        "חשון", "מרחשון" -> JewishDate.CHESHVAN
        "כסלו"            -> JewishDate.KISLEV
        "טבת"             -> JewishDate.TEVES
        "שבט"             -> JewishDate.SHEVAT
        "אדר", "אדר א", "אדר ב" -> JewishDate.ADAR
        else              -> null
    }
}
