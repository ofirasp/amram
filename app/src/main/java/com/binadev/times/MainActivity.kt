package com.binadev.times

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.binadev.times.R
import com.binadev.times.data.*
import com.binadev.times.ui.ContentArea
import com.binadev.times.ui.MemoEditorScreen
import com.binadev.times.ui.PrayerTimesPanel
import com.binadev.times.ui.SettingsScreen
import com.binadev.times.ui.SidePanel
import com.binadev.times.ui.theme.Black
import com.binadev.times.ui.theme.Gold
import com.binadev.times.ui.theme.NavyDark
import com.binadev.times.ui.theme.TextSecondary
import com.binadev.times.ui.theme.TimesTheme
import com.binadev.times.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private enum class Screen { MAIN, SETTINGS, MEMO_EDITOR }

class MainActivity : ComponentActivity() {

    private val openSettingsState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val initialSettings = SettingsStore.load(this)
        setContent {
            TimesTheme {
                SynagogueApp(
                    initialSettings = initialSettings,
                    openSettingsRequest = openSettingsState,
                )
            }
        }
    }

    // Enable long-press tracking for DPAD_CENTER on the main screen.
    // On the settings screen, focused Compose elements consume DPAD_CENTER
    // first, so this handler is never reached — no interference with navigation.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            openSettingsState.value = true
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SynagogueApp(initialSettings: AppSettings, openSettingsRequest: MutableState<Boolean>) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.MAIN) }
    var settings by remember { mutableStateOf(initialSettings) }

    // React to long-press signal from the Activity
    LaunchedEffect(openSettingsRequest.value) {
        if (openSettingsRequest.value) {
            screen = Screen.SETTINGS
            openSettingsRequest.value = false
        }
    }

    var yahrtzeitReloadKey by remember { mutableIntStateOf(0) }

    BackHandler(enabled = screen == Screen.SETTINGS) {
        screen = Screen.MAIN
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(200))) },
        label = "screen",
    ) { currentScreen ->
        when (currentScreen) {
            Screen.MAIN -> SynagogueScreen(settings = settings, yahrtzeitReloadKey = yahrtzeitReloadKey)
            Screen.SETTINGS -> SettingsScreen(
                currentSettings = settings,
                onSave = { newSettings ->
                    settings = newSettings
                    SettingsStore.save(context, newSettings)
                    screen = Screen.MAIN
                },
                onDismiss = { screen = Screen.MAIN },
                onOpenMemoEditor = { screen = Screen.MEMO_EDITOR },
            )
            Screen.MEMO_EDITOR -> MemoEditorScreen(
                onDismiss = { screen = Screen.SETTINGS },
                onSaved = { yahrtzeitReloadKey++ },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SynagogueScreen(settings: AppSettings, yahrtzeitReloadKey: Int = 0) {
    val context = LocalContext.current
    var slideIndex by remember { mutableIntStateOf(0) }
    var currentTime by remember { mutableStateOf("") }
    var daily by remember { mutableStateOf(DailyCalculations()) }
    var yahrzeits by remember { mutableStateOf<List<YahrtzeitEntry>>(emptyList()) }
    var moedSlides by remember { mutableStateOf<List<ContentSlide>>(emptyList()) }
    val slides = remember(settings.announcements, yahrzeits, moedSlides) {
        SynagogueData.buildSlides(settings.announcements, yahrzeits, moedSlides)
    }

    // ACTION_TIME_TICK fires every minute — use it as the reliable clock tick
    var timeTick by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: Intent) {
                timeTick++
            }
        }
        val filter = IntentFilter(Intent.ACTION_TIME_TICK).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }

    // Recalculate zmanim + Hebrew date on every minute tick
    LaunchedEffect(settings, timeTick) {
        daily = withContext(Dispatchers.Default) {
            ZmanimCalculator.calculateAll(settings)
        }
    }

    // Reload yahrzeits + moed slides every minute tick — guarantees update when day changes at Tzait
    LaunchedEffect(settings, yahrtzeitReloadKey, timeTick) {
        val tzaitMs = daily.tzaitMs
        yahrzeits = withContext(Dispatchers.IO) {
            YahrtzeitLoader.load(context, settings.testDateTime, tzaitMs)
        }
        moedSlides = withContext(Dispatchers.IO) {
            if (settings.showMoedSlides) MoedLoader.load(context, settings.testDateTime, tzaitMs) else emptyList()
        }
    }

    // Auto-advance slides every 8 seconds — restarts when slide count changes to re-capture slides
    LaunchedEffect(slides.size) {
        if (slideIndex >= slides.size) slideIndex = 0
        while (true) {
            delay(8_000)
            slideIndex = (slideIndex + 1) % slides.size
        }
    }

    // Live clock — frozen at test date when override is active
    LaunchedEffect(settings.testDateTime) {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        if (settings.testDateTime != null) {
            currentTime = fmt.format(Date(settings.testDateTime))
        } else {
            while (true) {
                currentTime = fmt.format(Date())
                delay(1_000)
            }
        }
    }

    // Pixel shift — cycles 9 positions every 30s to distribute pixel wear
    val shiftOffsets = remember {
        listOf(IntOffset(0,0), IntOffset(1,0), IntOffset(1,1), IntOffset(0,1),
               IntOffset(-1,1), IntOffset(-1,0), IntOffset(-1,-1), IntOffset(0,-1), IntOffset(1,-1))
    }
    var shiftIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            shiftIndex = (shiftIndex + 1) % shiftOffsets.size
        }
    }

    // Night dimming 22:00–4:00 at 30% brightness
    val activity = LocalContext.current as? android.app.Activity
    LaunchedEffect(timeTick) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 22 || hour < 4
        activity?.window?.let { win ->
            val attrs = win.attributes
            attrs.screenBrightness =
                if (isNight) 0.3f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            win.attributes = attrs
        }
    }

    Box(modifier = Modifier.fillMaxSize().offset { shiftOffsets[shiftIndex] }) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val h = maxHeight
        val w = maxWidth

        // Background
        Image(
            painter = painterResource(id = R.drawable.master),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )

        // ── Header box: top 30% of screen — center = 15% from top = inside ellipse
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(h * 0.30f),
        ) {
            // Synagogue title — top of header banner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (settings.titleLine1.isNotEmpty()) {
                    Text(
                        text = settings.titleLine1,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        textAlign = TextAlign.Center,
                    )
                }
                if (settings.titleLine2.isNotEmpty()) {
                    Text(
                        text = settings.titleLine2,
                        fontSize = 12.sp,
                        color = White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = w * 0.10f),
                    )
                }
            }

            // Date + time — center of this box hits the ellipse
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = listOfNotNull(
                        daily.hebrewInfo.dayOfWeek.takeIf { it.isNotEmpty() },
                        daily.hebrewInfo.hebrewDate.takeIf { it.isNotEmpty() },
                    ).joinToString(" | "),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-5).dp),
                )
                Text(
                    text = currentTime,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = 2.dp),
                )
            }

            // הדף היומי — left side of header
            if (daily.hebrewInfo.dafYomi.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(y = (-18).dp)
                        .padding(start = w * 0.09f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "הדף היומי", fontSize = 10.sp, color = White.copy(alpha = 0.7f))
                    Text(
                        text = daily.hebrewInfo.dafYomi,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // פרשת השבוע + הפטרה — right side of header
            if (daily.hebrewInfo.parasha.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = (-18).dp, x = (-40).dp)
                        .padding(end = w * 0.05f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (daily.hebrewInfo.haftara.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "הפטרה", fontSize = 10.sp, color = White.copy(alpha = 0.7f))
                            Text(
                                text = daily.hebrewInfo.haftara,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "פרשת השבוע", fontSize = 10.sp, color = White.copy(alpha = 0.7f))
                        Text(
                            text = daily.hebrewInfo.parasha,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // ── Main content — within the wooden inner frame ──────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = h * 0.25f,
                    start = w * 0.06f,
                    end = w * 0.06f,
                    bottom = h * 0.12f,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Left: זמני תפילות
            Box(modifier = Modifier.weight(0.22f).fillMaxHeight()) {
                PrayerTimesPanel(
                    weekdayPrayers = daily.weekdayPrayers,
                    shabbatPrayers = daily.shabbatPrayers,
                    dafYomi = daily.hebrewInfo.dafYomi,
                    fastName = daily.hebrewInfo.fastName,
                    fastStart = daily.hebrewInfo.fastStart,
                    fastEnd = daily.hebrewInfo.fastEnd,
                    holidayLabel = daily.hebrewInfo.holidayLabel,
                )
            }

            // Center: rotating slides
            Box(modifier = Modifier.weight(0.56f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = slideIndex,
                    transitionSpec = {
                        (fadeIn(tween(600)) + slideInHorizontally(tween(600)) { -it / 4 })
                            .togetherWith(fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { it / 4 })
                    },
                    label = "slide",
                ) { idx ->
                    ContentArea(
                        slide = slides[idx],
                        slideIndex = idx,
                        totalSlides = slides.size,
                    )
                }
            }

            // Right: זמני היום
            Box(modifier = Modifier.weight(0.22f).fillMaxHeight()) {
                SidePanel(
                    zmanim = daily.zmanim,
                    hebrewInfo = daily.hebrewInfo,
                    settings = settings,
                )
            }
        }

        // Fixed memorial footer in the brown frame area
        Text(
            text = "פיתוח התכנה לע\"נ מרים בת יוכבד זינו",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = h * 0.02f),
        )
    }
    } // end pixel-shift Box
}
