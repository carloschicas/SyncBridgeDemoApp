package com.syncbridge.demo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncbridge.demo.data.local.OrderDao
import com.syncbridge.demo.data.local.OrderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import io.syncbridge.SyncBridge
import io.syncbridge.conflict.TxnState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val syncBridge: SyncBridge
) : ViewModel() {

    val orders: StateFlow<List<OrderEntity>> = orderDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val queueSize: StateFlow<Int> = orderDao.observeAll()
        .map { list -> list.count { it.syncStatus == "PENDING" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun insertOrder(clientName: String, productName: String, quantity: Int) {
        viewModelScope.launch {
            val transactionId = UUID.randomUUID().toString()
            orderDao.insert(
                OrderEntity(
                    id = transactionId,
                    clientName = clientName,
                    productName = productName,
                    quantity = quantity,
                    syncStatus = "PENDING"
                )
            )

            val payload = """{"clientName":"$clientName","productName":"$productName","quantity":$quantity,"transactionId":"$transactionId"}"""
            val txnId = syncBridge.enqueue(
                endpoint = "/orders",
                payload = payload
            )

            syncBridge.observeTransaction(txnId).collect { state ->
                when (state) {
                    is TxnState.Synced -> orderDao.updateStatus(transactionId, "SYNCED")
                    is TxnState.Conflict -> orderDao.updateStatus(transactionId, "CONFLICT")
                    is TxnState.Dead -> orderDao.updateStatus(transactionId, "FAILED")
                    else -> Unit
                }
            }
        }
    }
}
