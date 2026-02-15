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

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.RecompositionMode.Immediate
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

@ExperimentalCoroutinesApi
class DebounceTest {
  @Test fun initialValueEmittedImmediately() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)
    var value: Int? = null

    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      rememberDebounced(42, 500.milliseconds)
    }

    assertThat(value).isEqualTo(42)

    job.cancelAndJoin()
  }

  @Test fun valueDoesNotPropagateBeforeTimeout() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)
    var value: Int? = null

    var input by mutableIntStateOf(0)
    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      rememberDebounced(input, 500.milliseconds)
    }

    assertThat(value).isEqualTo(0)

    input = 1
    runCurrent()
    clock.sendFrame(0)

    // Advance less than the timeout.
    advanceTimeBy(200)
    runCurrent()
    clock.sendFrame(0)

    assertThat(value).isEqualTo(0)

    job.cancelAndJoin()
  }

  @Test fun valuePropagatesAfterTimeout() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)
    var value: Int? = null

    var input by mutableIntStateOf(0)
    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      rememberDebounced(input, 500.milliseconds)
    }

    assertThat(value).isEqualTo(0)

    input = 1
    runCurrent()
    clock.sendFrame(0)

    advanceTimeBy(500)
    runCurrent()
    clock.sendFrame(0)

    assertThat(value).isEqualTo(1)

    job.cancelAndJoin()
  }

  @Test fun rapidChangesOnlyEmitLastValue() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)
    var value: Int? = null

    var input by mutableIntStateOf(0)
    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      rememberDebounced(input, 500.milliseconds)
    }

    assertThat(value).isEqualTo(0)

    // Rapid changes: each change resets the timer.
    input = 1
    runCurrent()
    clock.sendFrame(0)
    advanceTimeBy(200)
    runCurrent()
    clock.sendFrame(0)

    input = 2
    runCurrent()
    clock.sendFrame(0)
    advanceTimeBy(200)
    runCurrent()
    clock.sendFrame(0)

    input = 3
    runCurrent()
    clock.sendFrame(0)
    advanceTimeBy(200)
    runCurrent()
    clock.sendFrame(0)

    // Still showing initial value because timer kept resetting.
    assertThat(value).isEqualTo(0)

    // Now wait for the full timeout after last change.
    advanceTimeBy(300)
    runCurrent()
    clock.sendFrame(0)

    assertThat(value).isEqualTo(3)

    job.cancelAndJoin()
  }

  @Test fun zeroDurationPropagatesImmediately() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)
    var value: Int? = null

    var input by mutableIntStateOf(0)
    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      rememberDebounced(input, 0.milliseconds)
    }

    assertThat(value).isEqualTo(0)

    input = 1
    runCurrent()
    clock.sendFrame(0)
    runCurrent()
    clock.sendFrame(0)

    assertThat(value).isEqualTo(1)

    job.cancelAndJoin()
  }

  @Test fun initialValueEmittedImmediatelyImmediate() = runTest {
    val values = Channel<Int>()

    val job = launch {
      moleculeFlow(mode = Immediate) {
        rememberDebounced(42, 500.milliseconds)
      }.collect { values.send(it) }
    }

    assertThat(values.awaitValue()).isEqualTo(42)

    job.cancelAndJoin()
  }

  @Test fun valuePropagatesAfterTimeoutImmediate() = runTest {
    val values = Channel<Int>(Channel.UNLIMITED)

    var input by mutableIntStateOf(0)
    val job = launch {
      moleculeFlow(mode = Immediate) {
        rememberDebounced(input, 500.milliseconds)
      }.collect { values.send(it) }
    }

    assertThat(values.awaitValue()).isEqualTo(0)

    input = 1
    advanceTimeBy(500)
    runCurrent()

    // Drain intermediate recomposition emissions to get the final debounced value.
    assertThat(values.drainToLast()).isEqualTo(1)

    job.cancelAndJoin()
  }

  @Test fun rapidChangesOnlyEmitLastValueImmediate() = runTest {
    val values = Channel<Int>(Channel.UNLIMITED)

    var input by mutableIntStateOf(0)
    val job = launch {
      moleculeFlow(mode = Immediate) {
        rememberDebounced(input, 500.milliseconds)
      }.collect { values.send(it) }
    }

    assertThat(values.awaitValue()).isEqualTo(0)

    input = 1
    advanceTimeBy(200)
    runCurrent()

    input = 2
    advanceTimeBy(200)
    runCurrent()

    input = 3
    advanceTimeBy(500)
    runCurrent()

    // Drain intermediate recomposition emissions to get the final debounced value.
    assertThat(values.drainToLast()).isEqualTo(3)

    job.cancelAndJoin()
  }

  private suspend fun <T> Channel<T>.awaitValue(): T = withTimeout(1000) { receive() }

  private suspend fun <T> Channel<T>.drainToLast(): T {
    var last = withTimeout(1000) { receive() }
    while (true) {
      val result = tryReceive()
      if (result.isSuccess) last = result.getOrThrow() else break
    }
    return last
  }
}
