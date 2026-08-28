package com.lifetrack

import android.app.Application
import com.lifetrack.core.data.AppContainer
import com.lifetrack.notification.Notifier
import com.lifetrack.notification.work.DigestScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LifeTrackApplication : Application() {

    lateinit var container: AppContainer
        private set

    /** Application-scoped work that must outlive any screen. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.ensureChannel(this)
        // Re-arm on every launch. enqueueUniqueWork(REPLACE) makes this idempotent,
        // and it recovers scheduling if the chain was ever broken.
        appScope.launch { DigestScheduler.scheduleNext(this@LifeTrackApplication) }
    }
}
