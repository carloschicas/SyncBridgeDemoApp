package com.syncbridge.demo.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class OrderRequest(
    val clientName: String,
    val productName: String,
    val quantity: Int,
    val transactionId: String,
)

data class OrderResponse(
    val id: String,
    val status: String,
    val message: String,
)

interface ApiService {

    @POST("api/orders")
    suspend fun createOrder(@Body order: OrderRequest): Response<OrderResponse>

    @POST("api/orders/force-conflict")
    suspend fun createOrderForceConflict(@Body order: OrderRequest): Response<OrderResponse>
}
