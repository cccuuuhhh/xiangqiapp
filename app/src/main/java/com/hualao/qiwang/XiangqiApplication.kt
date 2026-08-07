package com.hualao.qiwang

import android.app.Application

/**
 * Application 入口 — 负责全局初始化
 */
class XiangqiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: XiangqiApplication
            private set
    }
}
