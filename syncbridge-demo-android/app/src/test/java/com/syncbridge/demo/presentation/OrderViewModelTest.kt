package com.syncbridge.demo.presentation

import com.syncbridge.demo.data.local.OrderDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
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
            syncBridge.enqueue(
                endpoint = "/api/orders",
                payload = any(),
                metadata = isNull(),
                httpMethod = any(),
                priority = any(),
                groupId = isNull(),
                ttlSeconds = isNull(),
                headers = any()
            )
        }
    }
}
