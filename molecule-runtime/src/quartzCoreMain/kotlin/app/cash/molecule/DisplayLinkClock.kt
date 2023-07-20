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
import platform.darwin.NSObject

public actual object DisplayLinkClock : MonotonicFrameClock {

  private val target = SelectorTarget(this)
  private val displayLink = CADisplayLink.displayLinkWithTarget(
    target = target,
    selector = NSSelectorFromString(SelectorTarget::tickClock.name),
  )

  private val clock = BroadcastFrameClock {
    // We only want to listen to the DisplayLink run loop if we have frame awaiters.
    displayLink.addToRunLoop(NSRunLoop.currentRunLoop, NSRunLoop.currentRunLoop.currentMode)
  }

  override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
    return clock.withFrameNanos(onFrame)
  }

  private fun tickClock() {
    clock.sendFrame(nanoTime())

    // Detach from the run loop. We will re-attach if new frame awaiters appear.
    displayLink.removeFromRunLoop(NSRunLoop.currentRunLoop, NSRunLoop.currentRunLoop.currentMode)
  }

  /**
   * Selectors can only target subtypes of [NSObject] which is why this helper exists.
   * We cannot subclass it on [DisplayLinkClock] directly because it implements a Kotlin
   * interface, but we also don't want to leak it into public API. The contained function
   * must be public for the selector to work.
   */
  private class SelectorTarget(private val target: DisplayLinkClock) : NSObject() {
    @ObjCAction
    fun tickClock() {
      target.tickClock()
    }
  }
}
