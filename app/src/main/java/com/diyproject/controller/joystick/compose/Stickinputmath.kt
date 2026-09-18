package com.diyproject.controller.joystick.compose

import kotlin.math.abs
import kotlin.math.sign

/**
 * Shared "transmitter feel" shaping applied to raw -1f..1f stick input
 * before it becomes a channel value — the same kind of deadzone/expo
 * settings you'd find in a FlySky transmitter's setup menu, so a
 * slightly-off-center resting stick doesn't creep the car, and small
 * movements near center give fine control while full deflection still
 * reaches 100%.
 *
 * The constants below are this controller's defaults. Both
 * [JoystickActivity] (for the actual channel math) and [JoystickScreen]
 * (for the stick widgets' deadzone-ring visuals) read from here, so the
 * two can't drift out of sync.
 */
object StickInputMath {

    const val STEERING_DEADZONE = 0.08f
    const val STEERING_EXPO = 0.35f
    const val THROTTLE_DEADZONE = 0.08f
    const val THROTTLE_EXPO = 0.25f
    const val GIMBAL_DEADZONE = 0.05f
    const val GIMBAL_EXPO = 0.20f

    /**
     * Clips travel inside [deadzone] to exactly 0, then rescales the
     * remaining travel back out to the full -1f..1f range so there's no
     * dead gap right past the deadzone boundary.
     */
    fun applyDeadzone(value: Float, deadzone: Float): Float {
        val v = value.coerceIn(-1f, 1f)
        val dz = deadzone.coerceIn(0f, 0.9f)
        if (abs(v) <= dz) return 0f
        return sign(v) * ((abs(v) - dz) / (1f - dz))
    }

    /**
     * Cubic exponential curve: softens response near center for finer
     * low-speed control while full deflection still maps to full output.
     * [expo] 0 = linear, 1 = fully cubic.
     */
    fun applyExpo(value: Float, expo: Float): Float {
        val v = value.coerceIn(-1f, 1f)
        val e = expo.coerceIn(0f, 1f)
        return e * v * v * v + (1f - e) * v
    }

    /** Deadzone, then expo, then scale to an integer channel in -range..range. */
    fun toChannel(raw: Float, deadzone: Float, expo: Float, range: Int = 100): Int {
        val shaped = applyExpo(applyDeadzone(raw, deadzone), expo)
        return (shaped * range).toInt().coerceIn(-range, range)
    }

    /** Deadzone, then expo, then map -1f..1f to a servo-style 0..180 angle. */
    fun toServoAngle(raw: Float, deadzone: Float, expo: Float): Int {
        val shaped = applyExpo(applyDeadzone(raw, deadzone), expo)
        return (90 + shaped * 90).toInt().coerceIn(0, 180)
    }
}