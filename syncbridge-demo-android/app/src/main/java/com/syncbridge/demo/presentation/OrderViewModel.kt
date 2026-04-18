package com.syncbridge.demo.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncbridge.demo.data.local.OrderDao
import com.syncbridge.demo.data.local.OrderEntity
import com.syncbridge.demo.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.syncbridge.SyncBridge
import io.syncbridge.conflict.TxnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val TAG = "SyncBridge"

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val syncBridge: SyncBridge,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    fun insertOrder(clientName: String, productName: String, quantity: Int) {
        viewModelScope.launch {
            val transactionId = UUID.randomUUID().toString()
            val totalAmount = quantity * 150.0

            Log.i(TAG, "INSERT  | txn=$transactionId | client=$clientName | product=$productName | qty=$quantity")

            orderDao.insert(
                OrderEntity(
                    id = transactionId,
                    clientName = clientName,
                    productName = productName,
                    quantity = quantity,
                    syncStatus = "PENDING"
                )
            )
            Log.d(TAG, "ROOM    | txn=$transactionId | saved PENDING")

            val payload = """{"customerName":"$clientName","productName":"$productName","quantity":$quantity,"totalAmount":$totalAmount}"""

            Log.i(TAG, "ENQUEUE | txn=$transactionId | POST /api/orders | payload=$payload")
            val txnId = try {
                syncBridge.enqueue(
                    endpoint = "/api/orders",
                    payload = payload,
                    headers = mapOf("X-Transaction-Id" to transactionId)
                )
            } catch (e: Exception) {
                Log.e(TAG, "ENQUEUE | txn=$transactionId | FAILED to enqueue: ${e.message}", e)
                return@launch
            }
            Log.i(TAG, "ENQUEUE | txn=$transactionId | accepted by SDK → sdkTxnId=$txnId")

            appScope.launch {
                syncBridge.observeTransaction(txnId).collect { state ->
                    when (state) {
                        is TxnState.Pending  -> Log.d(TAG, "STATE   | sdkTxnId=$txnId | PENDING")
                        is TxnState.Sending  -> Log.i(TAG, "STATE   | sdkTxnId=$txnId | SENDING → POST /api/orders")
                        is TxnState.Synced   -> {
                            Log.i(TAG, "STATE   | sdkTxnId=$txnId | SYNCED ✓")
                            orderDao.updateStatus(transactionId, "SYNCED")
                            Log.d(TAG, "ROOM    | txn=$transactionId | updated SYNCED")
                        }
                        is TxnState.Conflict -> {
                            Log.w(TAG, "STATE   | sdkTxnId=$txnId | CONFLICT 409 | ${state.serverMessage}")
                            orderDao.updateStatus(transactionId, "CONFLICT")
                            Log.d(TAG, "ROOM    | txn=$transactionId | updated CONFLICT")
                        }
                        is TxnState.Failed   -> Log.w(TAG, "STATE   | sdkTxnId=$txnId | FAILED (retrying) | ${state.reason}")
                        is TxnState.Dead     -> {
                            Log.e(TAG, "STATE   | sdkTxnId=$txnId | DEAD — no more retries")
                            orderDao.updateStatus(transactionId, "FAILED")
                            Log.d(TAG, "ROOM    | txn=$transactionId | updated FAILED")
                        }
                    }
                }
            }
        }
    }
}
