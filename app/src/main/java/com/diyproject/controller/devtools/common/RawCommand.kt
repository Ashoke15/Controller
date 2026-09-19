package com.diyproject.controller.devtools.common

object RawCommand {
    const val ARDUINO_DEFAULT_SERIAL_BUFFER_BYTES = 64

    fun withLineEnding(text: String, ending: LineEnding): String = text + ending.suffix

    fun byteLength(text: String): Int = text.toByteArray(Charsets.UTF_8).size
}