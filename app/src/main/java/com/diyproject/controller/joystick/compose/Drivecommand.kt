package com.diyproject.controller.joystick.compose

/**
 * Continuous, proportional drive command for the left stick — steering
 * and throttle are independent analog channels (-100..100), streamed
 * continuously rather than snapped to 8 directions plus a separate
 * speed value. This is what makes the left stick behave like a real RC
 * transmitter's throttle/rudder gimbal instead of a D-pad.
 *
 * Kept separate from `CarCommand` (lights/horn/warning/stop, which are
 * still simple on-off tokens) for the same reason `GimbalCommand` is
 * separate: I haven't seen CarCommand.kt, so this doesn't touch it.
 *
 * ⚠️ Still a placeholder wire format — verify against your firmware.
 * -100 = full reverse / full left, +100 = full forward / full right.
 * If your Arduino-side parser expects something else (e.g. PWM
 * microseconds like 1000-2000), change the two format strings below —
 * everything upstream already deals in the -100..100 range via
 * [StickInputMath], so only this file needs to change.
 */
object DriveCommand {
    fun steering(value: Int): String = "STR:${value.coerceIn(-100, 100)}\n"
    fun throttle(value: Int): String = "THR:${value.coerceIn(-100, 100)}\n"
}