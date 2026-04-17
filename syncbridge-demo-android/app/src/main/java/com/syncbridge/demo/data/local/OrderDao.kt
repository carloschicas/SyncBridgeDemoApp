package com.syncbridge.demo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Query("SELECT * FROM orders ORDER BY rowid DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("UPDATE orders SET syncStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
