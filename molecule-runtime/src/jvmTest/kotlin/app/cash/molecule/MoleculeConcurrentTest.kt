/*
 * Copyright (C) 2025 Square, Inc.
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

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode.Immediate
import assertk.assertThat
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest

// These tests are JVM only because they look at the current thread ID. It could be supported on
// all platforms with threads, but all the code is common, so this just gets us coverage quickly.
class MoleculeConcurrentTest {
  @Test fun coroutineContextHonoredByImmediateClock() = runTest {
    val testThread = Thread.currentThread()
    var firstThread: Thread? = null
    var secondThread: Thread? = null

    val job = Job()
    val cancelLatch = CompletableDeferred<Unit>()
    backgroundScope.launchMolecule(Immediate, job + Dispatchers.Default) {
      var count by remember { mutableIntStateOf(0) }
      when (count) {
        0 -> firstThread = Thread.currentThread()
        1 -> secondThread = Thread.currentThread()
      }
      if (count == 1) {
        cancelLatch.complete(Unit)
      }
      SideEffect {
        count++
      }
    }
    cancelLatch.await()

    assertThat(firstThread).isSameInstanceAs(testThread)
    assertThat(secondThread).isNotSameInstanceAs(testThread)

    job.cancelAndJoin()
  }
}
