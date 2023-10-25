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

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.molecule.MoleculeTest.DisposableEffectState.DISPOSED
import app.cash.molecule.MoleculeTest.DisposableEffectState.LAUNCHED
import app.cash.molecule.MoleculeTest.DisposableEffectState.NOT_LAUNCHED
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.RecompositionMode.Immediate
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameAs
import assertk.assertions.isSameAs
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

@ExperimentalCoroutinesApi
class MoleculeTest {
  @Test fun items() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)
    var value: Int? = null

    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(100)
          count++
        }
      }

      count
    }

    assertThat(value).isEqualTo(0)

    clock.sendFrame(0)
    assertThat(value).isEqualTo(0)

    advanceTimeBy(99)
    runCurrent()
    clock.sendFrame(0)
    assertThat(value).isEqualTo(0)

    advanceTimeBy(1)
    runCurrent()
    clock.sendFrame(0)
    assertThat(value).isEqualTo(1)

    advanceTimeBy(100)
    runCurrent()
    clock.sendFrame(0)
    assertThat(value).isEqualTo(2)

    job.cancel()
  }

  @Test fun errorImmediately() {
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(clock)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    assertFailure {
      scope.launchMolecule(ContextClock, emitter = { fail() }) {
        throw runtimeException
      }
    }.isSameAs(runtimeException)

    scope.cancel()
  }

  class RecordingExceptionHandler : CoroutineExceptionHandler {
    private val _exceptions = mutableListOf<Throwable>()
    val exceptions get() = _exceptions
    override fun handleException(context: CoroutineContext, exception: Throwable) {
      _exceptions += exception
    }
    override val key get() = CoroutineExceptionHandler
  }

  @Test fun errorDelayed() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(coroutineContext + job + clock + exceptionHandler)
    var value: Int? = null

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    var count by mutableStateOf(0)
    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      if (count == 1) {
        throw runtimeException
      }
      count
    }

    assertThat(value).isEqualTo(0)

    count++
    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
    runCurrent()
    clock.sendFrame(0)
    runCurrent()
    assertThat(exceptionHandler.exceptions.single()).isSameAs(runtimeException)

    job.cancel()
  }

  @Test fun errorInEffect() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(coroutineContext + job + clock + exceptionHandler)
    var value: Int? = null

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    scope.launchMolecule(ContextClock, emitter = { value = it }) {
      LaunchedEffect(Unit) {
        delay(50)
        throw runtimeException
      }
      0
    }

    assertThat(value).isEqualTo(0)

    advanceTimeBy(50)
    runCurrent()
    clock.sendFrame(0)
    assertThat(exceptionHandler.exceptions.single()).isSameAs(runtimeException)

    job.cancel()
  }

  @Test fun errorInEmitterImmediately() {
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(clock)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    assertFailure {
      scope.launchMolecule(ContextClock, emitter = { throw runtimeException }) {
        0
      }
    }.isSameAs(runtimeException)

    scope.cancel()
  }

  @Test fun errorInEmitterDelayed() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(coroutineContext + job + clock + exceptionHandler)
    var value: Int? = null

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    var count by mutableStateOf(0)
    scope.launchMolecule(
      ContextClock,
      emitter = {
        if (it == 1) {
          throw runtimeException
        }
        value = it
      },
    ) {
      count
    }

    assertThat(value).isEqualTo(0)

    count++
    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
    runCurrent()
    clock.sendFrame(0)
    runCurrent()
    assertThat(exceptionHandler.exceptions.single()).isSameAs(runtimeException)

    job.cancel()
  }

  enum class DisposableEffectState { NOT_LAUNCHED, LAUNCHED, DISPOSED }

  @Test fun disposableEffectDisposesWhenScopeIsCancelled() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)

    var state: DisposableEffectState = NOT_LAUNCHED

    scope.launchMolecule(ContextClock) {
      DisposableEffect(Unit) {
        state = LAUNCHED

        onDispose {
          state = DISPOSED
        }
      }
    }

    assertThat(state).isEqualTo(LAUNCHED)

    job.cancel()
    runCurrent()
    assertThat(state).isEqualTo(DISPOSED)
  }

  @Test
  fun itemsImmediate() = runTest {
    val values = Channel<Int>()

    val job = launch {
      moleculeFlow(mode = Immediate) {
        var count by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
          while (true) {
            delay(100)
            count++
          }
        }

        count
      }.collect { values.send(it) }
    }

    var value = values.awaitValue()
    assertThat(value).isEqualTo(0)

    advanceTimeBy(100)
    value = values.awaitValue()
    assertThat(value).isEqualTo(1)

    advanceTimeBy(100)
    value = values.awaitValue()
    assertThat(value).isEqualTo(2)

    advanceTimeBy(300)

    value = values.awaitValue()
    assertThat(value).isEqualTo(3)
    value = values.awaitValue()
    assertThat(value).isEqualTo(5)

    job.cancel()
  }

  @Test
  fun errorsImmediate() = runTest {
    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    assertFailure {
      moleculeFlow(mode = Immediate) {
        throw runtimeException
      }.collect()
    }.isSameAs(runtimeException)
  }

  @Test
  fun errorDelayedImmediate() = runTest {
    val values = Channel<Int>(Channel.UNLIMITED)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    var count by mutableStateOf(0)
    launch {
      val exception = kotlin.runCatching {
        moleculeFlow(mode = Immediate) {
          if (count == 1) {
            throw runtimeException
          }
          count
        }.collect {
          values.send(it)
        }
      }.exceptionOrNull()
      assertThat(runtimeException).isSameAs(exception)
    }

    assertThat(values.awaitValue()).isEqualTo(0)

    count++
    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
  }

  @Test
  fun errorInEffectImmediate() = runTest {
    val values = Channel<Int>(Channel.UNLIMITED)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    launch {
      val exception = kotlin.runCatching {
        moleculeFlow(mode = Immediate) {
          LaunchedEffect(Unit) {
            delay(50)
            throw runtimeException
          }
          0
        }.collect {
          values.send(it)
        }
      }.exceptionOrNull()
      assertThat(runtimeException).isSameAs(exception)
    }

    assertThat(values.awaitValue()).isEqualTo(0)

    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
  }

  @Test
  fun disposableEffectDisposesWhenScopeIsCancelledImmediate() = runTest {
    val values = Channel<Int>(Channel.UNLIMITED)

    var state: DisposableEffectState = NOT_LAUNCHED

    val job = launch {
      moleculeFlow(mode = Immediate) {
        DisposableEffect(Unit) {
          state = LAUNCHED

          onDispose {
            state = DISPOSED
          }
        }
        0
      }.collect {
        values.send(it)
      }
    }

    assertThat(values.awaitValue()).isEqualTo(0)

    job.cancelAndJoin()

    assertThat(state).isEqualTo(DISPOSED)
  }

  @Test fun coroutineContextUsed() = runTest {
    val expectedName = CoroutineName("test_key")

    var actualName: CoroutineName? = null
    backgroundScope.launchMolecule(Immediate, expectedName) {
      actualName = rememberCoroutineScope().coroutineContext[CoroutineName]
    }
    assertThat(actualName).isEqualTo(expectedName)
  }

  @Test fun coroutineContextClockDoesNotOverrideImmediate() = runTest {
    val myClock = BroadcastFrameClock()

    var actualClock: MonotonicFrameClock? = null
    backgroundScope.launchMolecule(Immediate, myClock) {
      actualClock = rememberCoroutineScope().coroutineContext[MonotonicFrameClock]
    }
    assertThat(actualClock).isNotSameAs(myClock)
  }

  private suspend fun <T> Channel<T>.awaitValue(): T = withTimeout(1000) { receive() }
}
