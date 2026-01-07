package com.example.individ_project

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class AppNotification(
    val title: String,
    val message: String
)

object NotificationCenter {

    // Было: MutableSharedFlow(extraBufferCapacity = 10)
    // Стало: с replay = 1, чтобы последнее событие не терялось,
    // пока MainActivity не успела подписаться.
    private val _events = MutableSharedFlow<AppNotification>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val events = _events.asSharedFlow()

    fun post(notification: AppNotification) {
        _events.tryEmit(notification)
    }
}
