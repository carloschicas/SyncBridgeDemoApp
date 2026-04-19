package com.syncbridge.demo.presentation

import com.syncbridge.demo.data.local.OrderDao
import android.util.Log
import io.mockk.*
import io.syncbridge.SyncBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var orderDao: OrderDao
    private lateinit var syncBridge: SyncBridge
    private lateinit var viewModel: OrderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        orderDao = mockk(relaxed = true)
        syncBridge = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun whenSavingOrder_thenEnqueuesToSyncBridge() = runTest {
        val mockTxnId = "mock-uuid-123"

        // All 8 args positional: avoids Kotlin's enqueue$default which NPEs on a mock
        // (config.getDefaultPriority() is null). ViewModel now passes all args explicitly.
        coEvery {
            syncBridge.enqueue(any(), any(), any(), any(), any(), any(), any(), any())
        } returns mockTxnId

        every { syncBridge.observeTransaction(mockTxnId) } returns emptyFlow()

        viewModel = OrderViewModel(
            orderDao = orderDao,
            syncBridge = syncBridge,
            appScope = this
        )

        viewModel.insertOrder(
            clientName = "Test Client",
            productName = "Test Product",
            quantity = 2
        )

        advanceUntilIdle()

        coVerify(exactly = 1) {
            syncBridge.enqueue(eq("/api/orders"), any(), any(), any(), any(), any(), any(), any())
        }
    }
}
