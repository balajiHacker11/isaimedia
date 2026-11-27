package com.isai.app

import android.app.Application
import com.isai.app.core.AppContainer

class IsaiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
