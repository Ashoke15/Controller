package com.diyproject.controller.joystick.compose

/**
 * Command encoding for the pan/tilt gimbal axis driven by the right
 * joystick, kept separate from `CarCommand` (drivetrain + lights) so it
 * doesn't need edits to a file I haven't seen.
 *
 * Confirmed from the compiler: `BluetoothSppManager.send()` takes a
 * `String` (same as `CarCommand.FORWARD` / `CarCommand.speedToChar()`),
 * so this sends plain text tokens rather than raw bytes/chars.
 *
 * ⚠️ Still a placeholder wire format — verify against your firmware.
 * "PAN:<0-180>\n" / "TLT:<0-180>\n" are deliberately distinct, human-
 * readable tokens chosen to avoid colliding with whatever short codes
 * CarCommand already uses (e.g. "F", "B", speed codes). Adjust the
 * prefixes/format to match your Arduino-side parser, or paste
 * CarCommand.kt and I'll align them exactly.
 */
object GimbalCommand {
    fun pan(angleDeg: Int): String = "PAN:${angleDeg.coerceIn(0, 180)}\n"
    fun tilt(angleDeg: Int): String = "TLT:${angleDeg.coerceIn(0, 180)}\n"
}