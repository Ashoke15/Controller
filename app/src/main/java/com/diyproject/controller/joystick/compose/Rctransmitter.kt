package com.diyproject.controller.joystick.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RcTransmitter(
    private val scope: CoroutineScope,
    private val send: (String) -> Unit,
    private val frameIntervalMs: Long = 40L,
    private val heartbeatEveryNFrames: Int = 25,
    private val onCommandSent: (String) -> Unit = {}
) {
    @Volatile private var steering = 0
    @Volatile private var throttle = 0
    @Volatile private var pan = 90
    @Volatile private var tilt = 90

    private var lastSentSteering = UNSENT
    private var lastSentThrottle = UNSENT
    private var lastSentPan = UNSENT
    private var lastSentTilt = UNSENT
    private var framesSinceHeartbeat = 0

    private var job: Job? = null

    fun setSteering(value: Int) { steering = value }
    fun setThrottle(value: Int) { throttle = value }
    fun setPan(value: Int) { pan = value }
    fun setTilt(value: Int) { tilt = value }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                tick()
                delay(frameIntervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun resendAll() {
        lastSentSteering = UNSENT
        lastSentThrottle = UNSENT
        lastSentPan = UNSENT
        lastSentTilt = UNSENT
    }

    fun sendNeutralNow() {
        steering = 0; throttle = 0; pan = 90; tilt = 90
        sendLogged(DriveCommand.steering(0))
        sendLogged(DriveCommand.throttle(0))
        sendLogged(GimbalCommand.pan(90))
        sendLogged(GimbalCommand.tilt(90))
        lastSentSteering = 0; lastSentThrottle = 0; lastSentPan = 90; lastSentTilt = 90
        framesSinceHeartbeat = 0
    }

    private fun tick() {
        val heartbeat = ++framesSinceHeartbeat >= heartbeatEveryNFrames
        if (heartbeat) framesSinceHeartbeat = 0

        val s = steering; val t = throttle; val p = pan; val ti = tilt

        if (heartbeat || s != lastSentSteering) { sendLogged(DriveCommand.steering(s)); lastSentSteering = s }
        if (heartbeat || t != lastSentThrottle) { sendLogged(DriveCommand.throttle(t)); lastSentThrottle = t }
        if (heartbeat || p != lastSentPan) { sendLogged(GimbalCommand.pan(p)); lastSentPan = p }
        if (heartbeat || ti != lastSentTilt) { sendLogged(GimbalCommand.tilt(ti)); lastSentTilt = ti }
    }

    private fun sendLogged(line: String) {
        send(line)
        onCommandSent(line)
    }

    private companion object {
        const val UNSENT = Int.MIN_VALUE
    }
}