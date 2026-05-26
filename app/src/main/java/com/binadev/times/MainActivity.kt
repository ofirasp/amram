package com.binadev.times

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.binadev.times.data.*
import com.binadev.times.ui.ContentArea
import com.binadev.times.ui.PrayerTimesPanel
import com.binadev.times.ui.SettingsScreen
import com.binadev.times.ui.SidePanel
import com.binadev.times.ui.theme.Gold
import com.binadev.times.ui.theme.NavyDark
import com.binadev.times.ui.theme.TextSecondary
import com.binadev.times.ui.theme.TimesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private enum class Screen { MAIN, SETTINGS }

class MainActivity : ComponentActivity() {

    private val openSettingsState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    BackHandler(enabled = screen == Screen.SETTINGS) {
        screen = Screen.MAIN
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(200))) },
        label = "screen",
    ) { currentScreen ->
        when (currentScreen) {
            Screen.MAIN -> SynagogueScreen(settings = settings)
            Screen.SETTINGS -> SettingsScreen(
                currentSettings = settings,
                onSave = { newSettings ->
                    settings = newSettings
                    SettingsStore.save(context, newSettings)
                    screen = Screen.MAIN
                },
                onDismiss = { screen = Screen.MAIN },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SynagogueScreen(settings: AppSettings) {
    val context = LocalContext.current
    var slideIndex by remember { mutableIntStateOf(0) }
    var currentTime by remember { mutableStateOf("") }
    var daily by remember { mutableStateOf(DailyCalculations()) }
    var yahrzeits by remember { mutableStateOf<List<YahrtzeitEntry>>(emptyList()) }
    var moedSlides by remember { mutableStateOf<List<ContentSlide>>(emptyList()) }
    val slides = remember(settings.announcements, yahrzeits, moedSlides) {
        SynagogueData.buildSlides(settings.announcements, yahrzeits, moedSlides)
    }

    // Recalculate zmanim + load yahrzeits + load moed slides when settings change, refresh every hour
    LaunchedEffect(settings) {
        while (true) {
            daily = withContext(Dispatchers.Default) {
                ZmanimCalculator.calculateAll(settings)
            }
            yahrzeits = withContext(Dispatchers.IO) {
                YahrtzeitLoader.load(context, settings.testDateTime)
            }
            moedSlides = withContext(Dispatchers.IO) {
                MoedLoader.load(context, settings.testDateTime)
            }
            delay(60 * 60 * 1_000L)
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        // Left: Prayer times — 20%
        Box(modifier = Modifier.weight(0.20f).fillMaxHeight()) {
            PrayerTimesPanel(
                weekdayPrayers = daily.weekdayPrayers,
                shabbatPrayers = daily.shabbatPrayers,
            )
        }

        // Center: Clock + Hebrew date at top + rotating slides — 45%
        Column(modifier = Modifier.weight(0.45f).fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDark)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = currentTime,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = daily.hebrewInfo.hebrewDate,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
        }

        // Right: Zmanim + Hebrew calendar info — 35%
        Box(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
            SidePanel(
                zmanim = daily.zmanim,
                hebrewInfo = daily.hebrewInfo,
                settings = settings,
            )
        }
    }
}
