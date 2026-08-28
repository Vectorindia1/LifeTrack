package com.lifetrack

import android.app.Application
import com.lifetrack.core.data.AppContainer

class LifeTrackApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
