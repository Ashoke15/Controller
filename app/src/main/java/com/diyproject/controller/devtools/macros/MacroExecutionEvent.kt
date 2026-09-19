package com.diyproject.controller.devtools.macros

sealed class MacroExecutionEvent {
    data class Started(val macroId: String, val totalSteps: Int) : MacroExecutionEvent()
    data class StepStarted(val macroId: String, val stepIndex: Int, val step: MacroStep) : MacroExecutionEvent()
    data class Finished(val macroId: String) : MacroExecutionEvent()
    object Stopped : MacroExecutionEvent()
}