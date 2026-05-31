package com.binadev.times.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.binadev.times.data.TefilaItem
import com.binadev.times.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PrayerTimesPanel(
    weekdayPrayers: List<TefilaItem>,
    shabbatPrayers: List<TefilaItem>,
    dafYomi: String = "",
    fastName: String = "",
    fastStart: String = "",
    fastEnd: String = "",
    holidayLabel: String = "",
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(NavyDark.copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            PrayerSectionHeader("זמני תפילות לחול")
            weekdayPrayers.forEach { PrayerRow(it) }

            PrayerDivider()

            PrayerSectionHeader("זמני תפילות לשבת")
            shabbatPrayers.forEach { PrayerRow(it) }

            if (fastName.isNotEmpty()) {
                PrayerDivider()
                PrayerSectionHeader("צום $fastName")
                if (fastStart.isNotEmpty()) PrayerRow(TefilaItem("כניסת הצום", fastStart))
                if (fastEnd.isNotEmpty())   PrayerRow(TefilaItem("יציאת הצום", fastEnd))
            } else if (holidayLabel.isNotEmpty()) {
                PrayerDivider()
                PrayerSectionHeader(holidayLabel)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PrayerSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Gold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PrayerRow(item: TefilaItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = item.label, fontSize = 13.sp, color = White)
        Text(text = item.time, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
    }
}

@Composable
private fun PrayerDivider() {
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NavyDivider)
    )
    Spacer(modifier = Modifier.height(4.dp))
}
