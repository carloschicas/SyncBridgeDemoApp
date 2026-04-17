package com.syncbridge.demo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.syncbridge.SyncBridge
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncBridgeModule {

    @Provides
    @Singleton
    fun provideSyncBridge(): SyncBridge = SyncBridge.getInstance()
}
