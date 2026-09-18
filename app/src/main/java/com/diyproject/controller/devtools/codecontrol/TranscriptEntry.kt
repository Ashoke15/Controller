package com.diyproject.controller.devtools.codecontrol

enum class TranscriptDirection { TX, RX, SYSTEM }

/** One line in the console — a sent command, a received line, or a
 *  connection-lifecycle notice. [raw] is exactly what was sent/received;
 *  display formatting (making control chars visible) is the UI's job,
 *  not this model's. */
data class TranscriptEntry(
    val id: Long,
    val direction: TranscriptDirection,
    val raw: String,
    val timestampMs: Long
)