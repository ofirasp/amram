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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.binadev.times.data.*
import com.binadev.times.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidePanel(
    zmanim: List<ZmanItem>,
    hebrewInfo: HebrewCalendarInfo,
    settings: AppSettings,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(NavyDark.copy(alpha = 0.60f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            // Zmanim
            SideSectionHeader("זמני היום")
            zmanim.forEach { SideTimeRow(it) }

            SideDivider()

            // Seasonal flags
            if (hebrewInfo.isMashivHaruach) FlagChip("משיב הרוח")
            else FlagChip("מוריד הטל")
            if (hebrewInfo.isWinterBlessingSeason) FlagChip("ברך עלינו") else FlagChip("ברכנו")
            if (hebrewInfo.isYaaleVeyavo) FlagChip("יעלה ויבוא")
            if (hebrewInfo.isAlHaNisim)  FlagChip("על הניסים")
            if (hebrewInfo.isShabbatMevorchim) FlagChip("שבת מברכים")
            if (hebrewInfo.moladText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = hebrewInfo.moladText,
                    fontSize = 11.5.sp,
                    lineHeight = 13.sp,
                    color = White.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (hebrewInfo.isTachanunOmitted) FlagChip("א\"א תחנון")
            if (hebrewInfo.isTzidkatchaOmitted) FlagChip("א\"א צדקתך")

            // Omer
            if (hebrewInfo.omerText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ספירת העומר",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = hebrewInfo.omerText,
                    fontSize = 11.5.sp,
                    lineHeight = 11.sp,
                    color = White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SideSectionHeader(title: String) {
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
private fun SideTimeRow(item: ZmanItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = item.label, fontSize = 13.sp, color = White, textAlign = TextAlign.End)
        Text(text = item.time, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FlagChip(text: String) {
    Text(
        text = "• $text",
        fontSize = 13.sp,
        color = White.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SideDivider() {
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(White.copy(alpha = 0.25f))
    )
    Spacer(modifier = Modifier.height(4.dp))
}
