package com.binadev.times.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.binadev.times.data.MemoRepository
import com.binadev.times.data.YahrtzeitEntry
import com.binadev.times.ui.theme.*

@Stable
class MemoRow(date: String, motherName: String, relationship: String, name: String, yearly: Boolean = false) {
    var date by mutableStateOf(date)
    var motherName by mutableStateOf(motherName)
    var relationship by mutableStateOf(relationship)
    var name by mutableStateOf(name)
    var yearly by mutableStateOf(yearly)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MemoEditorScreen(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var selectedMonth by remember { mutableStateOf(MemoRepository.allMonths[0]) }
    val rows = remember { mutableStateListOf<MemoRow>() }
    val firstFocus = remember { FocusRequester() }

    // Accumulates edits for every month visited — non-state to avoid recomposition
    val edits = remember { mutableMapOf<String, List<MemoRow>>() }
    val loadedMonthRef = remember { object { var value: String? = null } }

    LaunchedEffect(selectedMonth) {
        // Snapshot the month we're leaving before clearing rows
        loadedMonthRef.value?.let { prev ->
            edits[prev] = rows.map { MemoRow(it.date, it.motherName, it.relationship, it.name, it.yearly) }
        }
        rows.clear()
        val cached = edits[selectedMonth]
        if (cached != null) {
            rows.addAll(cached)
        } else {
            MemoRepository.load(context, selectedMonth).forEach { entry ->
                rows.add(MemoRow(entry.date, entry.motherName, entry.relationship, entry.name, entry.yearly))
            }
        }
        loadedMonthRef.value = selectedMonth
        runCatching { firstFocus.requestFocus() }
    }

    fun saveAllAndExit() {
        // Snapshot current month into edits before saving
        loadedMonthRef.value?.let { current ->
            edits[current] = rows.map { MemoRow(it.date, it.motherName, it.relationship, it.name, it.yearly) }
        }
        edits.forEach { (month, monthRows) ->
            MemoRepository.save(context, month,
                monthRows.map { YahrtzeitEntry(
                    name = it.name,
                    relationship = it.relationship,
                    motherName = it.motherName,
                    date = it.date,
                    yearly = it.yearly,
                ) }
            )
        }
        onSaved()
        onDismiss()
    }

    BackHandler(onBack = ::saveAllAndExit)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NavyDark.copy(alpha = 0.97f))
                .padding(horizontal = 32.dp, vertical = 20.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "עריכת השכבות – $selectedMonth",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
                EditorButton(text = "שמור וצא", isAccent = true, onClick = ::saveAllAndExit)
            }

            Spacer(Modifier.height(12.dp))

            // ── Month selector ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MemoRepository.allMonths.forEachIndexed { index, month ->
                    MonthTab(
                        text = month,
                        isSelected = month == selectedMonth,
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        onClick = { selectedMonth = month },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Column headers ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("תאריך",   modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold, textAlign = TextAlign.Center)
                Text("שם",     modifier = Modifier.weight(2.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold, textAlign = TextAlign.Center)
                Text("קשר",    modifier = Modifier.weight(1f),   fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold, textAlign = TextAlign.Center)
                Text("שם",     modifier = Modifier.weight(2f),   fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold, textAlign = TextAlign.Center)
                Text("שנה",    modifier = Modifier.width(36.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gold, textAlign = TextAlign.Center)
                Spacer(Modifier.width(42.dp))
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(Gold.copy(alpha = 0.4f)))
            Spacer(Modifier.height(4.dp))

            // ── Rows ──────────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(rows) { index, row ->
                    MemoRowEditor(
                        row = row,
                        onDelete = { rows.removeAt(index) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Gold.copy(alpha = 0.2f)))
            Spacer(Modifier.height(8.dp))

            // ── Footer ────────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorButton(text = "+ הוסף שורה", isAccent = false) {
                    rows.add(MemoRow("", "", "", ""))
                }
                Text(
                    text = "${rows.size} רשומות",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MemoRowEditor(row: MemoRow, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(NavyPanel)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemoCell(value = row.date,         onValueChange = { row.date = it },         modifier = Modifier.weight(1.5f))
        MemoCell(value = row.name,         onValueChange = { row.name = it },         modifier = Modifier.weight(2.5f))
        MemoCell(value = row.relationship, onValueChange = { row.relationship = it }, modifier = Modifier.weight(1f))
        MemoCell(value = row.motherName,   onValueChange = { row.motherName = it },   modifier = Modifier.weight(2f))
        YearlyCheckbox(checked = row.yearly, onCheckedChange = { row.yearly = it })
        DeleteBtn(onClick = onDelete)
    }
}

@Composable
private fun MemoCell(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) NavyDivider else NavyDark)
            .border(
                width = if (isFocused) 1.5.dp else 0.5.dp,
                color = if (isFocused) Gold else NavyDivider,
                shape = RoundedCornerShape(4.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        textStyle = TextStyle(
            color = TextPrimary,
            fontSize = 13.sp,
            textDirection = TextDirection.Rtl,
            textAlign = TextAlign.Start,
        ),
        cursorBrush = SolidColor(Gold),
        singleLine = true,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun YearlyCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(when {
                checked && isFocused -> GoldLight
                checked              -> Gold
                isFocused            -> NavyDivider
                else                 -> NavyDark
            })
            .border(1.dp, if (isFocused || checked) Gold else NavyDivider, RoundedCornerShape(4.dp))
            .focusable(interactionSource = interaction)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Text("✓", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = if (isFocused) NavyPanel else NavyDark)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DeleteBtn(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) NavyDivider else NavyPanel)
            .border(1.dp, if (isFocused) Gold else NavyDivider, RoundedCornerShape(6.dp))
            .focusable(interactionSource = interaction)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", fontSize = 14.sp, color = if (isFocused) Gold else TextMuted)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MonthTab(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(when { isSelected && isFocused -> GoldLight; isSelected -> Gold; isFocused -> NavyDivider; else -> NavyPanel })
            .border(1.dp, if (isFocused || isSelected) Gold else NavyDivider, RoundedCornerShape(6.dp))
            .focusable(interactionSource = interaction)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NavyDark else TextPrimary,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EditorButton(text: String, isAccent: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(when { isAccent && isFocused -> GoldLight; isAccent -> Gold; isFocused -> NavyDivider; else -> NavyPanel })
            .border(1.dp, if (isFocused || isAccent) Gold else NavyDivider, RoundedCornerShape(8.dp))
            .focusable(interactionSource = interaction)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) NavyDark else TextPrimary,
        )
    }
}
