package com.diyproject.controller.devtools.common

enum class TranscriptDirection { TX, RX, SYSTEM }

data class TranscriptEntry(
    val id: Long,
    val direction: TranscriptDirection,
    val raw: String,
    val timestampMs: Long
)