package com.syncbridge.demo.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncbridge.demo.data.local.OrderDao
import com.syncbridge.demo.data.local.OrderEntity
import com.syncbridge.demo.di.ConflictManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.syncbridge.SyncBridge
import io.syncbridge.conflict.ConflictEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SyncBridge"

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val syncBridge: SyncBridge,
    private val conflictManager: ConflictManager
) : ViewModel() {

    val orders: StateFlow<List<OrderEntity>> = orderDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val queueSize: StateFlow<Int> = orderDao.observeAll()
        .map { list -> list.count { it.syncStatus == "PENDING" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isOnline: StateFlow<Boolean> = syncBridge.networkState

    private val _conflictEvent = MutableStateFlow<ConflictEvent?>(null)
    val conflictEvent: StateFlow<ConflictEvent?> = _conflictEvent.asStateFlow()

    init {
        viewModelScope.launch {
            syncBridge.networkState
                .collect { online ->
                    if (online) Log.i(TAG, "NETWORK | ONLINE — SDK will drain queue")
                    else Log.w(TAG, "NETWORK | OFFLINE — SDK will queue requests locally")
                }
        }

        viewModelScope.launch {
            conflictManager.conflicts.collect { event ->
                Log.w(TAG, "CONFLICT| txn=${event.transaction.id} | serverResponse=${event.serverResponse}")
                _conflictEvent.value = event
            }
        }
    }

    fun dismissConflict() {
        Log.d(TAG, "CONFLICT| dismissed by user")
        _conflictEvent.value = null
    }
}
