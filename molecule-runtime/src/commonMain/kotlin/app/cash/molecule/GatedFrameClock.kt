/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.molecule

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.launch

/**
 * A [MonotonicFrameClock] that is either running, or not.
 *
 * While running, any request for a frame immediately succeeds. If stopped, requests for a frame wait until
 * the clock is set to run again.
 */
internal class GatedFrameClock(scope: CoroutineScope) : MonotonicFrameClock {
  private val frameSends = Channel<Unit>(CONFLATED)

  init {
    scope.launch {
      for (send in frameSends) sendFrame()
    }
  }

  var isRunning: Boolean = true
    set(value) {
      val started = value && !field
      field = value
      if (started) {
        sendFrame()
      }
    }

  private var lastNanos = 0L
  private var lastOffset = 0

  private fun sendFrame() {
    val timeNanos = nanoTime()

    // Since we only have millisecond resolution, ensure the nanos form always increases by
    // incrementing a nano offset if we collide with the previous timestamp.
    val offset = if (timeNanos == lastNanos) {
      lastOffset + 1
    } else {
      lastNanos = timeNanos
      0
    }
    lastOffset = offset

    clock.sendFrame(timeNanos + offset)
  }

  private val clock = BroadcastFrameClock {
    if (isRunning) frameSends.trySend(Unit).getOrThrow()
  }

  override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
    return clock.withFrameNanos(onFrame)
  }
}
