package com.syncbridge.demo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.syncbridge.SyncBridge
import io.syncbridge.adapters.room.RoomSyncAdapter
import io.syncbridge.adapters.room.SyncBridgeDatabase

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val syncDb = SyncBridgeDatabase.getInstance(this)
        SyncBridge.initialize(this) {
            storageAdapter(RoomSyncAdapter(syncDb.syncDao()))
            authTokenProvider { "demo-token" }
        }
    }
}
