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
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
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
import platform.CoreVideo.CVOptionFlags
import platform.CoreVideo.CVOptionFlagsVar
import platform.CoreVideo.CVTimeStamp
import platform.CoreVideo.kCVReturnSuccess

public actual object DisplayLinkClock : MonotonicFrameClock {

  private val clock = BroadcastFrameClock {
    // One or more awaiters have appeared. Start the DisplayLink clock callback so that awaiters
    // get dispatched on the next available frame.
    checkDisplayLink(CVDisplayLinkStart(displayLink.value))
  }
  private val clockPtr = StableRef.create(clock)

  // We alloc directly to nativeHeap because this singleton object lives for the duration of the
  // process. We don't care about cleanup and therefore never free this.
  private val displayLink = nativeHeap.alloc<CVDisplayLinkRefVar>()

  init {
    checkDisplayLink(CVDisplayLinkCreateWithActiveCGDisplays(displayLink.ptr))
    checkDisplayLink(
      CVDisplayLinkSetOutputCallback(
        displayLink.value,
        staticCFunction(::callback),
        clockPtr.asCPointer(),
      ),
    )
  }

  actual override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
    return clock.withFrameNanos(onFrame)
  }

  private fun checkDisplayLink(code: Int) {
    check(code == kCVReturnSuccess) { "Could not initialize CVDisplayLink. Error code $code." }
  }
}

@Suppress("UNUSED_PARAMETER") // Signature required by CVDisplayLinkSetOutputCallback.
private fun callback(
  displayLink: CVDisplayLinkRef?,
  ignored1: CPointer<CVTimeStamp>?,
  ignored2: CPointer<CVTimeStamp>?,
  ignored3: CVOptionFlags,
  ignored4: CPointer<CVOptionFlagsVar>?,
  userInfo: COpaquePointer?,
): Int {
  val clock = userInfo!!.asStableRef<BroadcastFrameClock>().get()
  clock.sendFrame(nanoTime())

  // A frame was delivered. Stop the DisplayLink callback. It will get started again
  // when new frame awaiters appear.
  return CVDisplayLinkStop(displayLink)
}
