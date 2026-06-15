package com.binadev.times.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.binadev.zmanim.R
import com.binadev.times.data.ContentSlide
import com.binadev.times.data.SlideContent
import com.binadev.times.data.YahrtzeitEntry
import com.binadev.times.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ContentArea(slide: ContentSlide, slideIndex: Int, totalSlides: Int, isCompact: Boolean = false) {
    val context = LocalContext.current
    val torahImage = remember(slide) {
        val asset = (slide.content as? SlideContent.TorahLesson)?.imageAsset
        if (asset != null) {
            runCatching {
                context.assets.open("pics/$asset").use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        } else null
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 36.dp)
            ) {
                // Slide title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = slide.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        textAlign = TextAlign.Start,
                    )
                    if (slide.content is SlideContent.Yahrzeits) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_candle),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    if (torahImage != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        Image(
                            bitmap = torahImage.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(52.dp).wrapContentWidth(),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Gold.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Slide-type-specific content
                when (val content = slide.content) {
                    is SlideContent.Announcements -> AnnouncementsContent(content, isCompact)
                    is SlideContent.TorahLesson -> TorahContent(slide.title, content)
                    is SlideContent.Yahrzeits -> YahrtzeitContent(content, isCompact)
                }
            }

        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AnnouncementsContent(content: SlideContent.Announcements, isCompact: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        content.items.forEach { announcement ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyDark.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = announcement,
                    fontSize = if (isCompact) 16.sp else 20.sp,
                    color = TextPrimary,
                    lineHeight = if (isCompact) 24.sp else 32.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TorahContent(title: String, content: SlideContent.TorahLesson) {
    val scrollState = rememberScrollState()
    var lineCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(lineCount) {
        if (lineCount > 6) {
            delay(3_000)
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(durationMillis = 8_000, easing = LinearEasing),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(NavyDark.copy(alpha = 0.85f))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
            Text(
                text = content.body,
                fontSize = 15.sp,
                color = TextPrimary,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                onTextLayout = { lineCount = it.lineCount },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun YahrtzeitContent(content: SlideContent.Yahrzeits, isCompact: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        content.entries.chunked(2).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowEntries.forEach { entry ->
                    YahrtzeitCard(entry, isCompact = isCompact, modifier = Modifier.weight(1f))
                }
                if (rowEntries.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun YahrtzeitCard(entry: YahrtzeitEntry, isCompact: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NavyDark.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.date,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldLight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${entry.name} ${entry.relationship} ${entry.motherName}",
                fontSize = if (isCompact) 15.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                textAlign = TextAlign.Center
            )
        }
    }
}
