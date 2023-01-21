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

  @ObjCAction public fun tickClock() {
    // Remove the DisplayLink from the run loop. It will get added again if new frame awaiters
    // appear.
    displayLink.removeFromRunLoop(NSRunLoop.currentRunLoop, NSRunLoop.currentRunLoop.currentMode)
    clock.sendFrame(0L)
  }
}
