/*
 * Copyright (C) 2021 Square, Inc.
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
import kotlin.coroutines.suspendCoroutine
import kotlinx.browser.window

public object WindowAnimationFrameClock : MonotonicFrameClock {
  override suspend fun <R> withFrameNanos(
    onFrame: (Long) -> R,
  ): R = suspendCoroutine { continuation ->
    window.requestAnimationFrame {
      val durationMillis = it.toLong()
      val durationNanos = durationMillis * 1_000_000
      val result = onFrame(durationNanos)
      continuation.resume(result)
    }
  }
}
