package com.diyproject.controller.devtools.codecontrol

/**
 * Line ending appended to whatever the developer typed — mirrors a real
 * serial terminal's "no line ending / NL / CR / CRLF" options, since
 * sketches vary in what Serial.readStringUntil()/readline() expects.
 * Unlike DriveCommand/GimbalCommand, this is adjustable per command
 * rather than hardcoded, because Code Control is for hardware that
 * doesn't have an established wire format yet.
 */
enum class LineEnding(val label: String, val suffix: String) {
    NONE("None", ""),
    NL("\\n", "\n"),
    CR("\\r", "\r"),
    CRLF("\\r\\n", "\r\n")
}

object RawCommand {
    /** Default Arduino HardwareSerial RX ring buffer size in bytes. A
     *  command longer than this can silently lose bytes on the receiving
     *  end before loop() drains it. Flagged in the UI, not enforced —
     *  some boards/cores raise this. */
    const val ARDUINO_DEFAULT_SERIAL_BUFFER_BYTES = 64

    fun withLineEnding(text: String, ending: LineEnding): String = text + ending.suffix

    fun byteLength(text: String): Int = text.toByteArray(Charsets.UTF_8).size
}