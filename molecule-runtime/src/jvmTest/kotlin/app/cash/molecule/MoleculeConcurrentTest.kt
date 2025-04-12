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

import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.molecule.RecompositionMode.Immediate
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

// These tests are JVM only because they look at the current thread ID. It could be supported on
// all platforms with threads, but all the code is common, so this just gets us coverage quickly.
@ExperimentalCoroutinesApi
class MoleculeConcurrentTest {
  @Test fun coroutineContextHonoredByImmediateClock() = runTest(UnconfinedTestDispatcher()) {
    val testThread = Thread.currentThread()

    val job = Job()
    val threadChannel = Channel<Thread>(Channel.UNLIMITED)
    lateinit var recomposeScope: RecomposeScope
    backgroundScope.launchMolecule(Immediate, job + Dispatchers.Default) {
      threadChannel.trySend(Thread.currentThread())
      // This can be used to manually trigger recompositions outside of the composable.
      val scope = currentRecomposeScope
      SideEffect { recomposeScope = scope }
    }

    val firstThread = threadChannel.receive()
    assertThat(firstThread).isSameInstanceAs(testThread)

    recomposeScope.invalidate()

    val secondThread = threadChannel.receive()
    assertThat(secondThread).isNotSameInstanceAs(testThread)
    assertThat(secondThread.name).contains("DefaultDispatcher")

    job.cancelAndJoin()
  }

  @Test fun coroutineContextHonoredByImmediateClockInEmitter() = runTest(UnconfinedTestDispatcher()) {
    val testThread = Thread.currentThread()

    val job = Job()
    val threadChannel = Channel<Thread>(Channel.UNLIMITED)
    var count by mutableIntStateOf(0)
    backgroundScope.launchMolecule(
      mode = Immediate,
      context = job + Dispatchers.Default,
      emitter = { threadChannel.trySend(Thread.currentThread()) },
    ) {
      count
    }

    val firstThread = threadChannel.receive()
    assertThat(firstThread).isSameInstanceAs(testThread)

    Snapshot.withMutableSnapshot { count++ }

    val secondThread = threadChannel.receive()
    assertThat(secondThread).isNotSameInstanceAs(testThread)
    assertThat(secondThread.name).contains("DefaultDispatcher")

    job.cancelAndJoin()
  }
}
