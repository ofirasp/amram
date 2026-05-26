package com.binadev.times.data

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.nio.charset.Charset
import java.util.Date

object YahrtzeitLoader {

    fun load(context: Context, testDateTimeMs: Long? = null): List<YahrtzeitEntry> {
        val cal = if (testDateTimeMs != null) JewishCalendar(Date(testDateTimeMs)) else JewishCalendar()
        val month = cal.jewishMonth
        val filename = monthFileName(month) ?: return emptyList()
        return try {
            context.assets.open("memo/$filename.csv")
                .bufferedReader(Charset.forName("windows-1255"))
                .readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { parseLine(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // CSV column order: date, motherName, relationship, name
    private fun parseLine(line: String): YahrtzeitEntry? {
        val parts = line.split(",")
        if (parts.size < 4) return null
        return YahrtzeitEntry(
            date         = parts[0].trim(),
            motherName   = parts[1].trim(),
            relationship = parts[2].trim(),
            name         = parts[3].trim(),
        )
    }

    private fun monthFileName(month: Int) = when (month) {
        JewishCalendar.NISSAN   -> "ניסן"
        JewishCalendar.IYAR     -> "אייר"
        JewishCalendar.SIVAN    -> "סיון"
        JewishCalendar.TAMMUZ   -> "תמוז"
        JewishCalendar.AV       -> "אב"
        JewishCalendar.ELUL     -> "אלול"
        JewishCalendar.TISHREI  -> "תשרי"
        JewishCalendar.CHESHVAN -> "חשון"
        JewishCalendar.KISLEV   -> "כסלו"
        JewishCalendar.TEVES    -> "טבת"
        JewishCalendar.SHEVAT   -> "שבט"
        JewishCalendar.ADAR     -> "אדר"
        JewishCalendar.ADAR_II  -> "אדר ב"
        else -> null
    }
}
