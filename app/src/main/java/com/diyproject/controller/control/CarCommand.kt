package com.diyproject.controller.control

object CarCommand {
    const val FORWARD = "F"
    const val BACK = "B"
    const val LEFT = "L"
    const val RIGHT = "R"
    const val FORWARD_LEFT = "G"
    const val FORWARD_RIGHT = "I"
    const val BACK_LEFT = "H"
    const val BACK_RIGHT = "J"
    const val STOP = "S"

    const val FRONT_LIGHTS_ON = "W"
    const val FRONT_LIGHTS_OFF = "w"
    const val BACK_LIGHTS_ON = "U"
    const val BACK_LIGHTS_OFF = "u"
    const val HORN_ON = "V"
    const val HORN_OFF = "v"
    const val EXTRA_ON = "X"   // hazard / red triangle indicator
    const val EXTRA_OFF = "x"
    const val STOP_ALL = "D"

    /** Speed 0-100 (in steps of 10) -> single char per firmware table.
     *  Rounds to the NEAREST step rather than flooring, so e.g. 95-100
     *  all resolve to max ("q") instead of only exactly 100. */
    fun speedToChar(speedPercent: Int): String {
        val clamped = speedPercent.coerceIn(0, 100)
        val step = ((clamped + 5) / 10).coerceIn(0, 10)
        return if (step == 10) "q" else step.toString()
    }
}