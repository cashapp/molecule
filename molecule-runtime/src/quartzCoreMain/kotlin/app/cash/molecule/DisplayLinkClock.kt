/*
 * Copyright (C) 2023 Square, Inc.
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
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CADisplayLink

public actual object DisplayLinkClock : MonotonicFrameClock {

  @Suppress("unused") // This registers a DisplayLink listener.
  private val displayLink: CADisplayLink = CADisplayLink.displayLinkWithTarget(
    target = this,
    selector = NSSelectorFromString(this::tickClock.name),
  )

  private val clock = BroadcastFrameClock {
    // We only want to listen to the DisplayLink run loop if we have frame awaiters.
    displayLink.addToRunLoop(NSRunLoop.currentRunLoop, NSRunLoop.currentRunLoop.currentMode)
  }

  override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
    return clock.withFrameNanos(onFrame)
  }

  // The following function must remain public to be a valid candidate for the call to
  // NSSelectorString above.
  @ObjCAction public fun tickClock() {
    clock.sendFrame(0L)

    // Remove the DisplayLink from the run loop. It will get added again if new frame awaiters
    // appear.
    displayLink.removeFromRunLoop(NSRunLoop.currentRunLoop, NSRunLoop.currentRunLoop.currentMode)
  }
}
