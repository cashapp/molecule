/*
 * Copyright (C) 2024 Square, Inc.
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

import androidx.compose.runtime.MonotonicFrameClock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

public object NodeJsFrameClock : MonotonicFrameClock {
  private var lastNanos: Long = 0L

  override suspend fun <R> withFrameNanos(
    onFrame: (Long) -> R,
  ): R = suspendCoroutine { continuation ->
    setImmediate {
      val now = nanoTime()
      val frameTimeNanos = if (now <= lastNanos) {
        lastNanos + 1L
      } else {
        now
      }
      lastNanos = frameTimeNanos
      try {
        val result = onFrame(frameTimeNanos)
        continuation.resume(result)
      } catch (t: Throwable) {
        continuation.resumeWithException(t)
      }
    }
  }
}

private external fun setImmediate(callback: () -> Unit)
