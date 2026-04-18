package com.syncbridge.demo.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var orderDao: OrderDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        orderDao = database.orderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertOrder_and_readInFlow() = runTest {
        val testOrder = OrderEntity(
            id = "order-test-001",
            clientName = "Carlos Chicas",
            productName = "Laptop Pro",
            quantity = 2,
            syncStatus = "PENDING"
        )

        orderDao.insert(testOrder)

        val orders = orderDao.observeAll().first()

        assertTrue(orders.isNotEmpty())
        assertEquals(1, orders.size)
        assertEquals(testOrder.id, orders[0].id)
        assertEquals(testOrder.clientName, orders[0].clientName)
        assertEquals(testOrder.productName, orders[0].productName)
        assertEquals(testOrder.quantity, orders[0].quantity)
        assertEquals(testOrder.syncStatus, orders[0].syncStatus)
    }
}
