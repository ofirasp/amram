package com.binadev.times.data

import android.content.Context
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.util.Calendar
import java.util.Date

object YahrtzeitLoader {

    fun load(context: Context, testDateTimeMs: Long? = null, tzaitMs: Long? = null): List<YahrtzeitEntry> {
        val nowMs = testDateTimeMs ?: System.currentTimeMillis()
        val cal = JewishCalendar(Date(nowMs))
        if (tzaitMs != null && nowMs > tzaitMs) cal.forward(Calendar.DATE, 1)
        val currentMonthFile = monthFileName(cal.jewishMonth) ?: return emptyList()
        val currentMonth = MemoRepository.load(context, currentMonthFile)
        val yearlyFromOthers = MemoRepository.allMonths
            .filter { it != currentMonthFile }
            .flatMap { month -> MemoRepository.load(context, month).filter { it.yearly } }
        return currentMonth + yearlyFromOthers
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
