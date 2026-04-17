package com.syncbridge.demo

import android.app.Application
import android.util.Log
import com.syncbridge.demo.di.ConflictManager
import dagger.hilt.android.HiltAndroidApp
import io.syncbridge.SyncBridge
import io.syncbridge.adapters.room.RoomSyncAdapter
import io.syncbridge.adapters.room.SyncBridgeDatabase
import javax.inject.Inject

private const val TAG = "SyncBridge"

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var conflictManager: ConflictManager

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "INIT    | Initializing SyncBridge SDK")
        val syncDb = SyncBridgeDatabase.getInstance(this)
        SyncBridge.initialize(this) {
            storageAdapter(RoomSyncAdapter(syncDb.syncDao()))
            authTokenProvider {
                Log.d(TAG, "AUTH    | Providing auth token")
                "demo-token"
            }
            conflictListener { event ->
                Log.w(TAG, "CONFLICT| txn=${event.transaction.id} | at=${event.conflictedAt} | response=${event.serverResponse}")
                conflictManager.emit(event)
            }
        }
        Log.i(TAG, "INIT    | SyncBridge SDK ready")
    }
}
