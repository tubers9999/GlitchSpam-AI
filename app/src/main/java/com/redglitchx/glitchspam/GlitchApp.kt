package com.redglitchx.glitchspam

import android.app.Application
import android.content.Context

class GlitchApp : Application() {

    companion object {
        lateinit var instance: GlitchApp
            private set

        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
