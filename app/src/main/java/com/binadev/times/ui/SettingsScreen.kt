package com.binadev.times.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.binadev.times.data.*
import com.binadev.times.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
    onOpenMemoEditor: () -> Unit = {},
) {
    var selectedCity    by remember { mutableStateOf(currentSettings.city) }
    var selectedSystem  by remember { mutableStateOf(currentSettings.prayerSystem) }
    var titleLine1 by remember { mutableStateOf(currentSettings.titleLine1) }
    var titleLine2 by remember { mutableStateOf(currentSettings.titleLine2) }
    val editableAnnouncements = remember {
        mutableStateListOf(*currentSettings.announcements.toTypedArray())
    }
    val firstFocus = remember { FocusRequester() }

    var showMoedSlides by remember { mutableStateOf(currentSettings.showMoedSlides) }
    var testDateEnabled by remember { mutableStateOf(currentSettings.testDateTime != null) }
    val initCal = remember {
        Calendar.getInstance().apply {
            if (currentSettings.testDateTime != null) timeInMillis = currentSettings.testDateTime
        }
    }
    var testYear   by remember { mutableIntStateOf(initCal.get(Calendar.YEAR)) }
    var testMonth  by remember { mutableIntStateOf(initCal.get(Calendar.MONTH) + 1) }
    var testDay    by remember { mutableIntStateOf(initCal.get(Calendar.DAY_OF_MONTH)) }
    var testHour   by remember { mutableIntStateOf(initCal.get(Calendar.HOUR_OF_DAY)) }
    var testMinute by remember { mutableIntStateOf(initCal.get(Calendar.MINUTE)) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    // ── Date/time picker — proper modal Dialog window ──────────────────────
    if (showDateTimePicker) {
        DateTimePickerDialog(
            year = testYear, month = testMonth, day = testDay,
            hour = testHour, minute = testMinute,
            onConfirm = { y, mo, d, h, mi ->
                testYear = y; testMonth = mo; testDay = d
                testHour = h; testMinute = mi
                testDateEnabled = true
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false },
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyDark.copy(alpha = 0.97f))
                .padding(40.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Title
                Text(
                    text = "הגדרות",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
                Text(
                    text = "לחץ OK לבחירה · לחץ חזור לביטול",
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {

                    // ── City selection ──────────────────────────────────────
                    Column(modifier = Modifier.weight(1f)) {
                        SettingsSectionHeader("בחר עיר")
                        PresetCities.forEachIndexed { index, city ->
                            SettingsOptionButton(
                                text = city.nameHebrew,
                                subtitle = city.nameEnglish,
                                isSelected = city.nameEnglish == selectedCity.nameEnglish,
                                modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                                onClick = { selectedCity = city },
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // ── Prayer system ────────────────────────────────────────
                    Column(modifier = Modifier.weight(1f)) {
                        SettingsSectionHeader("בחר מנהג")
                        PrayerSystem.entries.forEach { system ->
                            SettingsOptionButton(
                                text = system.hebrewName,
                                subtitle = systemDescription(system),
                                isSelected = system == selectedSystem,
                                onClick = { selectedSystem = system },
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyPanel)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("בחירה נוכחית:", fontSize = 11.sp, color = TextMuted)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = selectedCity.nameHebrew,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold,
                                )
                                Text(
                                    text = selectedSystem.hebrewName,
                                    fontSize = 14.sp,
                                    color = GoldLight,
                                )
                            }
                        }

                    }

                    // ── Title lines + Announcements editing ─────────────────
                    Column(
                        modifier = Modifier
                            .weight(2f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SettingsSectionHeader("כותרת כללית")
                        AnnouncementTextField(
                            value = titleLine1,
                            onValueChange = { titleLine1 = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )
                        AnnouncementTextField(
                            value = titleLine2,
                            onValueChange = { titleLine2 = it },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        )

                        SettingsSectionHeader("הודעות לציבור")

                        editableAnnouncements.forEachIndexed { index, text ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AnnouncementTextField(
                                    value = text,
                                    onValueChange = { editableAnnouncements[index] = it },
                                    modifier = Modifier.weight(1f),
                                )
                                DeleteButton(onClick = { editableAnnouncements.removeAt(index) })
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        ActionButton(
                            text = "+ הוסף הודעה",
                            isAccent = false,
                            onClick = { editableAnnouncements.add("") },
                        )
                    }
                }

                // ── Test date/time row ───────────────────────────────────────
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "מצב בדיקה:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )
                    if (testDateEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyPanel)
                                .border(1.dp, Gold, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "%02d/%02d/%04d  %02d:%02d".format(
                                    testDay, testMonth, testYear, testHour, testMinute
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gold,
                            )
                        }
                        Box(modifier = Modifier.width(140.dp)) {
                            ActionButton(text = "שנה", isAccent = false,
                                onClick = { showDateTimePicker = true })
                        }
                        Box(modifier = Modifier.width(160.dp)) {
                            ActionButton(text = "בטל מצב בדיקה", isAccent = false,
                                onClick = { testDateEnabled = false })
                        }
                    } else {
                        Box(modifier = Modifier.width(230.dp)) {
                            ActionButton(text = "הגדר תאריך/שעה ידניים", isAccent = false,
                                onClick = { showDateTimePicker = true })
                        }
                        Text(text = "כרגע: זמן מערכת", fontSize = 12.sp, color = TextMuted)
                    }
                }

                // ── Moed checkbox ────────────────────────────────────────────
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsCheckbox(
                            label = "הצג שקפי מועדים",
                            checked = showMoedSlides,
                            onToggle = { showMoedSlides = !showMoedSlides },
                        )
                    }
                    Spacer(modifier = Modifier.weight(2f))
                }

                // ── Save / Cancel / Memo editor footer ───────────────────────
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ActionButton(
                            text = "עריכת השכבות",
                            isAccent = false,
                            onClick = onOpenMemoEditor,
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ActionButton(
                            text = "שמור הגדרות",
                            isAccent = true,
                            onClick = {
                                val testMs = if (testDateEnabled) {
                                    Calendar.getInstance().apply {
                                        set(Calendar.YEAR, testYear)
                                        set(Calendar.MONTH, testMonth - 1)
                                        set(Calendar.DAY_OF_MONTH, testDay)
                                        set(Calendar.HOUR_OF_DAY, testHour)
                                        set(Calendar.MINUTE, testMinute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                } else null
                                onSave(AppSettings(
                                    city = selectedCity,
                                    prayerSystem = selectedSystem,
                                    announcements = editableAnnouncements.filter { it.isNotBlank() },
                                    testDateTime = testMs,
                                    titleLine1 = titleLine1,
                                    titleLine2 = titleLine2,
                                    showMoedSlides = showMoedSlides,
                                ))
                            },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ActionButton(text = "ביטול", isAccent = false, onClick = onDismiss)
                    }
                }
            }
        }
    }
}

// ── Date/time picker dialog ────────────────────────────────────────────────────
// Uses compose Dialog() which creates a proper Android window — focus is
// automatically trapped inside and back-press closes it.

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    year: Int, month: Int, day: Int,
    hour: Int, minute: Int,
    onConfirm: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickerYear   by remember { mutableIntStateOf(year) }
    var pickerMonth  by remember { mutableIntStateOf(month) }
    var pickerDay    by remember { mutableIntStateOf(day) }
    var pickerHour   by remember { mutableIntStateOf(hour) }
    var pickerMinute by remember { mutableIntStateOf(minute) }

    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NavyDark.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavyPanel)
                        .border(2.dp, Gold, RoundedCornerShape(16.dp))
                        .padding(32.dp)
                        .widthIn(min = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "בחר תאריך ושעה",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        modifier = Modifier.padding(bottom = 24.dp),
                    )

                    Text("תאריך", fontSize = 13.sp, color = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NumberStepper(
                            label = "יום", value = pickerDay, min = 1, max = 31, wrap = true,
                            firstFocusRequester = firstFocus, modifier = Modifier.width(90.dp),
                        ) { pickerDay = it }
                        NumberStepper(
                            label = "חודש", value = pickerMonth, min = 1, max = 12, wrap = true,
                            modifier = Modifier.width(90.dp),
                        ) { pickerMonth = it }
                        NumberStepper(
                            label = "שנה", value = pickerYear, min = 2000, max = 2099, wrap = false,
                            modifier = Modifier.width(110.dp),
                        ) { pickerYear = it }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("שעה", fontSize = 13.sp, color = TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        NumberStepper(
                            label = "שעה", value = pickerHour, min = 0, max = 23, wrap = true,
                            modifier = Modifier.width(90.dp),
                        ) { pickerHour = it }
                        NumberStepper(
                            label = "דקות", value = pickerMinute, min = 0, max = 59, wrap = true,
                            modifier = Modifier.width(90.dp),
                        ) { pickerMinute = it }
                    }

                    Spacer(Modifier.height(28.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.width(150.dp)) {
                            ActionButton(
                                text = "אישור",
                                isAccent = true,
                                onClick = {
                                    onConfirm(pickerYear, pickerMonth, pickerDay, pickerHour, pickerMinute)
                                },
                            )
                        }
                        Box(modifier = Modifier.width(150.dp)) {
                            ActionButton(text = "ביטול", isAccent = false, onClick = onDismiss)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    wrap: Boolean,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester? = null,
    onValueChange: (Int) -> Unit,
) {
    val interactionDec = remember { MutableInteractionSource() }
    val interactionInc = remember { MutableInteractionSource() }
    val isFocusedDec by interactionDec.collectIsFocusedAsState()
    val isFocusedInc by interactionInc.collectIsFocusedAsState()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isFocusedDec) NavyDivider else NavyPanel)
                    .border(1.dp, if (isFocusedDec) Gold else NavyDivider, RoundedCornerShape(4.dp))
                    .focusable(interactionSource = interactionDec)
                    .clickable { onValueChange(if (value <= min) (if (wrap) max else min) else value - 1) },
                contentAlignment = Alignment.Center,
            ) {
                Text("◄", fontSize = 13.sp, color = if (isFocusedDec) Gold else TextPrimary)
            }
            Text(
                text = value.toString().padStart(if (max >= 1000) 4 else 2, '0'),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = if (max >= 1000) 48.dp else 32.dp),
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isFocusedInc) NavyDivider else NavyPanel)
                    .border(1.dp, if (isFocusedInc) Gold else NavyDivider, RoundedCornerShape(4.dp))
                    .focusable(interactionSource = interactionInc)
                    .clickable { onValueChange(if (value >= max) (if (wrap) min else max) else value + 1) },
                contentAlignment = Alignment.Center,
            ) {
                Text("►", fontSize = 13.sp, color = if (isFocusedInc) Gold else TextPrimary)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AnnouncementTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) NavyDivider else NavyPanel)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Gold else NavyDivider,
                shape = RoundedCornerShape(8.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(12.dp),
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 13.sp,
            textDirection = TextDirection.Rtl,
        ),
        cursorBrush = SolidColor(Gold),
        minLines = 2,
        maxLines = 5,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeleteButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) NavyDivider else NavyPanel)
            .border(1.dp, if (isFocused) Gold else NavyDivider, RoundedCornerShape(6.dp))
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", fontSize = 16.sp, color = if (isFocused) Gold else TextMuted)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Gold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsOptionButton(
    text: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected && isFocused -> GoldLight
                    isSelected             -> Gold
                    isFocused              -> NavyDivider
                    else                   -> NavyPanel
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused || isSelected) Gold else NavyDivider,
                shape = RoundedCornerShape(8.dp),
            )
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) NavyDark else TextPrimary,
            )
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 11.sp, color = if (isSelected) NavyPanel else TextMuted)
            }
        }
        if (isSelected) {
            Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionButton(
    text: String,
    isAccent: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isAccent && isFocused -> GoldLight
                    isAccent             -> Gold
                    isFocused            -> NavyDivider
                    else                 -> NavyPanel
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused || isAccent) Gold else NavyDivider,
                shape = RoundedCornerShape(8.dp),
            )
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) NavyDark else TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsCheckbox(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) NavyDivider else NavyPanel)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Gold else NavyDivider,
                shape = RoundedCornerShape(8.dp),
            )
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = TextPrimary,
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) Gold else NavyDark)
                .border(1.dp, Gold, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        }
    }
}

private fun systemDescription(system: PrayerSystem) = when (system) {
    PrayerSystem.MIZRACHI -> "סוזק\"ש מג\"א · הדלקת נרות 20 דק'"
    PrayerSystem.ASHKENAZ -> "סוזק\"ש גר\"א · הדלקת נרות 18 דק'"
}
