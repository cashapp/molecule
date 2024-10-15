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

import assertk.all
import assertk.assertThat
import assertk.assertions.isLessThan
import assertk.assertions.isPositive
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalNativeApi::class)
class DisplayLinkClockTest {
  @Test fun ticksWithTime() = runTest {
    if (Platform.osFamily == OsFamily.IOS || Platform.osFamily == OsFamily.TVOS) {
      // TODO Link against XCTest in order to get frame pulses on iOS and tvOS.
      return@runTest
    }

    val frameTimeA = DisplayLinkClock.withFrameNanos { it }
    val frameTimeB = DisplayLinkClock.withFrameNanos { it }
    assertThat(frameTimeA).all {
      isPositive()
      isLessThan(frameTimeB)
    }
  }
}
