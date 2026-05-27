package com.binadev.times.data

// ── Shared model types ───────────────────────────────────────────────────────

data class ZmanItem(val label: String, val time: String)

data class TefilaItem(val label: String, val time: String)

data class YahrtzeitEntry(
    val name: String,
    val relationship: String,
    val motherName: String,
    val date: String,
    val yearly: Boolean = false,
)

data class HebrewCalendarInfo(
    val hebrewDate: String = "",
    val parasha: String = "",
    val haftara: String = "",
    val dafYomi: String = "",
    val isYaaleVeyavo: Boolean = false,
    val isMashivHaruach: Boolean = false,
    val isShabbatMevorchim: Boolean = false,
    val isWinterBlessingSeason: Boolean = false,
    val moladText: String = "",
    val omerText: String = "",
    val isTachanunOmitted: Boolean = false,
    val isTzidkatchaOmitted: Boolean = false,
)

// ── Slide content types ───────────────────────────────────────────────────────

sealed class SlideContent {
    data class Announcements(val items: List<String>) : SlideContent()
    data class TorahLesson(val body: String, val imageAsset: String? = null) : SlideContent()
    data class Yahrzeits(val entries: List<YahrtzeitEntry>) : SlideContent()
}

data class ContentSlide(val title: String, val content: SlideContent)

// ── Slide builder ─────────────────────────────────────────────────────────────

object SynagogueData {

    fun buildSlides(
        announcements: List<String>,
        yahrzeits: List<YahrtzeitEntry>,
        moedSlides: List<ContentSlide> = emptyList(),
    ): List<ContentSlide> = buildList {
        val nonEmpty = announcements.filter { it.isNotBlank() }
        if (nonEmpty.isNotEmpty()) {
            add(ContentSlide(
                title = "הודעות לציבור",
                content = SlideContent.Announcements(nonEmpty),
            ))
        }
        addAll(moedSlides)
        yahrzeits.chunked(6).forEach { chunk ->
            add(ContentSlide(
                title = "השכבה החודש – לעילוי נשמתם",
                content = SlideContent.Yahrzeits(chunk),
            ))
        }
    }
}
