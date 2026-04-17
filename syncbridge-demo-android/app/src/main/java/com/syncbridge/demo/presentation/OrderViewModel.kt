package com.syncbridge.demo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncbridge.demo.data.local.OrderDao
import com.syncbridge.demo.data.local.OrderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderDao: OrderDao
) : ViewModel() {

    val orders: StateFlow<List<OrderEntity>> = orderDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insertOrder(clientName: String, productName: String, quantity: Int) {
        viewModelScope.launch {
            orderDao.insert(
                OrderEntity(
                    id = UUID.randomUUID().toString(),
                    clientName = clientName,
                    productName = productName,
                    quantity = quantity,
                    syncStatus = "PENDING"
                )
            )
        }
    }
}
