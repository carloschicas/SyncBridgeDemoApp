package com.syncbridge.demo.di

import android.content.Context
import androidx.room.Room
import com.syncbridge.demo.data.local.AppDatabase
import com.syncbridge.demo.data.local.OrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "syncbridge_demo.db").build()

    @Provides
    @Singleton
    fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()
}
