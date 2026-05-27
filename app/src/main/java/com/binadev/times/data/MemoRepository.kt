package com.binadev.times.data

import android.content.Context
import java.io.File
import java.nio.charset.Charset

object MemoRepository {

    val allMonths = listOf(
        "תשרי", "חשון", "כסלו", "טבת", "שבט",
        "אדר", "אדר ב", "ניסן", "אייר", "סיון", "תמוז", "אב", "אלול"
    )

    fun load(context: Context, monthName: String): List<YahrtzeitEntry> {
        val internal = internalFile(context, monthName)
        return if (internal.exists()) {
            parseCsv(internal.readText(Charsets.UTF_8))
        } else {
            try {
                val bytes = context.assets.open("memo/$monthName.csv").readBytes()
                parseCsv(bytes.toString(Charset.forName("windows-1255")))
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun save(context: Context, monthName: String, entries: List<YahrtzeitEntry>) {
        val dir = File(context.filesDir, "memo")
        dir.mkdirs()
        internalFile(context, monthName).writeText(
            entries.joinToString("\n") { "${it.date},${it.motherName},${it.relationship},${it.name},${if (it.yearly) "1" else "0"}" },
            Charsets.UTF_8
        )
    }

    private fun internalFile(context: Context, monthName: String) =
        File(context.filesDir, "memo/$monthName.csv")

    private fun parseCsv(content: String): List<YahrtzeitEntry> =
        content.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split(",")
            if (p.size < 4) null
            // Hebrew dates always contain ' (e.g. "א' סיון"). If p[0] lacks it but p[3] has it,
            // the file was saved with swapped columns — auto-correct the order.
            else if (!p[0].trim().contains("'") && p[3].trim().contains("'")) {
                YahrtzeitEntry(
                    name = p[0].trim(),
                    relationship = p[1].trim(),
                    motherName = p[2].trim(),
                    date = p[3].trim(),
                    yearly = p.getOrNull(4)?.trim() == "1",
                )
            } else {
                YahrtzeitEntry(
                    date = p[0].trim(),
                    motherName = p[1].trim(),
                    relationship = p[2].trim(),
                    name = p[3].trim(),
                    yearly = p.getOrNull(4)?.trim() == "1",
                )
            }
        }
}
