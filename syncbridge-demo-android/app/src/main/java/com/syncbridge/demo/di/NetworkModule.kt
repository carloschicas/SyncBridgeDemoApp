package com.syncbridge.demo.di

import com.syncbridge.demo.data.network.ApiService
import com.syncbridge.demo.data.network.MockServerInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val MOCK_BASE_URL = "http://mock.syncbridge.local/"

    @Provides
    @Singleton
    fun provideMockServerInterceptor(): MockServerInterceptor = MockServerInterceptor()

    @Provides
    @Singleton
    fun provideOkHttpClient(interceptor: MockServerInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(MOCK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
