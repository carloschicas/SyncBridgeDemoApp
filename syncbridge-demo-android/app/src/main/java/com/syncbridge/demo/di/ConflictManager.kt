package com.syncbridge.demo.di

import io.syncbridge.conflict.ConflictEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConflictManager @Inject constructor() {

    private val _conflicts = MutableSharedFlow<ConflictEvent>()
    val conflicts: SharedFlow<ConflictEvent> = _conflicts.asSharedFlow()

    suspend fun emit(event: ConflictEvent) {
        _conflicts.emit(event)
    }
}
