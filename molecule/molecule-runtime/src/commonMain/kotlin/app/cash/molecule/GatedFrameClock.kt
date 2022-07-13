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
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * A [MonotonicFrameClock] that is either running, or not.
 *
 * While running, any request for a frame immediately succeeds. If stopped, requests for a frame wait until
 * the clock is set to run again.
 */
@OptIn(ExperimentalTime::class)
internal class GatedFrameClock : MonotonicFrameClock {
  private val start = TimeSource.Monotonic.markNow()

  var isRunning: Boolean = true
    set(value) {
      val started = value && !field
      field = value
      if (started) {
        sendFrame()
      }
    }

  private fun sendFrame() {
    clock.sendFrame(start.elapsedNow().inWholeNanoseconds)
  }

  private val clock = BroadcastFrameClock {
    if (isRunning) sendFrame()
  }

  override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
    return clock.withFrameNanos(onFrame)
  }
}
