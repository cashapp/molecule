package app.cash.molecule

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.CoreVideo.CVDisplayLinkCreateWithActiveCGDisplays
import platform.CoreVideo.CVDisplayLinkRef
import platform.CoreVideo.CVDisplayLinkRefVar
import platform.CoreVideo.CVDisplayLinkSetOutputCallback
import platform.CoreVideo.CVDisplayLinkStart
import platform.CoreVideo.CVDisplayLinkStop
import platform.CoreVideo.kCVReturnSuccess

public actual object DisplayLinkClock : MonotonicFrameClock {

  private val clock = BroadcastFrameClock {
    // One or more awaiters have appeared. Start the DisplayLink clock callback so that awaiters
    // get dispatched on the next available frame.
    checkDisplayLink(CVDisplayLinkStart(displayLink.value))
  }

  // We alloc directly to nativeHeap because this singleton object lives for the duration of the
  // process. We don't care about cleanup and therefore never free this.
  private val displayLink = nativeHeap.alloc<CVDisplayLinkRefVar>()

  init {
    checkDisplayLink(CVDisplayLinkCreateWithActiveCGDisplays(displayLink.ptr))
    checkDisplayLink(
      CVDisplayLinkSetOutputCallback(
        displayLink.value,
        staticCFunction { _, _, _, _, _, _ ->
          clock.sendFrame(0L)
          // A frame was delivered. Stop the DisplayLink callback. It will get started again
          // when new frame awaiters appear.
          CVDisplayLinkStop(displayLink.value)
        },
        null,
      ),
    )
  }

  override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
    return clock.withFrameNanos(onFrame)
  }
}

private fun checkDisplayLink(code: Int) {
  check(code == kCVReturnSuccess) { "Could not initialize CVDisplayLink. Error code $code." }
}
