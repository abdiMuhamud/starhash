package com.innovii.starhash

import android.app.Application
import com.innovii.starhash.core.DemoNetwork
import com.innovii.starhash.ussd.SmsWatcher

class StarHashApp : Application() {
    lateinit var store: Store
        private set
    lateinit var runs: RunController
        private set

    /** The demo network keeps its balance and subscriptions until the app closes or it is reset. */
    var demo = DemoNetwork(balance = DEMO_BALANCE)
        private set

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        runs = RunController(this)
        SmsWatcher.register(this)
    }

    fun resetDemo() {
        demo = DemoNetwork(balance = DEMO_BALANCE)
    }

    companion object {
        const val DEMO_BALANCE = 1.00
    }
}
