package com.music.vivi

import android.app.Application
import android.content.Intent
import android.os.Bundle
import java.lang.Thread.*

class CrashActivity(private val application: Application) : UncaughtExceptionHandler {
    override fun uncaughtException(p0: Thread, p1: Throwable) {
        TODO("Not yet implemented")
    }
    fun initHandler() {
        setDefaultUncaughtExceptionHandler(this)
    }
}