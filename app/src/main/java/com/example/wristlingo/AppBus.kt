package com.example.wristlingo

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

object AppBus {
  // Expose latest caption with replay so UI immediately shows last
  val captions = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}

