package com.syncbridge.demo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val clientName: String,
    val productName: String,
    val quantity: Int,
    val syncStatus: String = "PENDING"
)
