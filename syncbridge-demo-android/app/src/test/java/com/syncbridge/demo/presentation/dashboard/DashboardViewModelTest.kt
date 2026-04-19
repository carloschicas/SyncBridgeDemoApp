package com.syncbridge.demo.presentation.dashboard

import android.util.Log
import com.syncbridge.demo.data.local.OrderDao
import com.syncbridge.demo.di.ConflictManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.syncbridge.SyncBridge
import io.syncbridge.conflict.ConflictEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var orderDao: OrderDao
    private lateinit var syncBridge: SyncBridge
    private lateinit var conflictManager: ConflictManager
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        orderDao = mockk(relaxed = true)
        syncBridge = mockk()
        conflictManager = mockk()

        every { orderDao.observeAll() } returns flowOf(emptyList())
        every { syncBridge.networkState } returns MutableStateFlow(true)
        every { conflictManager.conflicts } returns MutableSharedFlow<ConflictEvent>()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun isOnline_isTrue_whenSdkReportsNetworkAvailable() = runTest {
        viewModel = DashboardViewModel(
            orderDao = orderDao,
            syncBridge = syncBridge,
            conflictManager = conflictManager
        )

        assertTrue(viewModel.isOnline.value)
    }
}
