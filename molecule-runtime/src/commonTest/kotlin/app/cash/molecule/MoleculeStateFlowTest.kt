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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import app.cash.molecule.RecompositionClock.ContextClock
import app.cash.molecule.RecompositionClock.Immediate
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@ExperimentalCoroutinesApi
class MoleculeStateFlowTest {
  @Test fun items() = runTest {
    val job = Job()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(coroutineContext + job + clock)

    val flow = scope.launchMolecule(ContextClock) {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(100)
          count++
        }
      }

      count
    }

    assertThat(flow.value).isEqualTo(0)

    clock.sendFrame(0)
    assertThat(flow.value).isEqualTo(0)

    advanceTimeBy(99)
    runCurrent()
    clock.sendFrame(0)
    assertThat(flow.value).isEqualTo(0)

    advanceTimeBy(1)
    runCurrent()
    clock.sendFrame(0)
    assertThat(flow.value).isEqualTo(1)

    advanceTimeBy(100)
    runCurrent()
    clock.sendFrame(0)
    assertThat(flow.value).isEqualTo(2)

    job.cancel()
  }

  @Test fun errorImmediately() {
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(clock)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    assertFailure {
      scope.launchMolecule(ContextClock) {
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

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    var count by mutableStateOf(0)
    val flow = scope.launchMolecule(ContextClock) {
      println("Sup $count")
      if (count == 1) {
        throw runtimeException
      }
      count
    }

    assertThat(flow.value).isEqualTo(0)

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

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    val flow = scope.launchMolecule(ContextClock) {
      LaunchedEffect(Unit) {
        delay(50)
        throw runtimeException
      }
      0
    }

    assertThat(flow.value).isEqualTo(0)

    advanceTimeBy(50)
    runCurrent()
    clock.sendFrame(0)
    assertThat(exceptionHandler.exceptions.single()).isSameAs(runtimeException)

    job.cancel()
  }

  @Test
  fun itemsImmediate() = runTest {
    val job = Job(coroutineContext.job)
    val scope = this + job

    val flow = scope.launchMolecule(Immediate) {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(100)
          count++
        }
      }

      count
    }

    assertThat(flow.value).isEqualTo(0)

    advanceTimeBy(99)
    runCurrent()
    assertThat(flow.value).isEqualTo(0)

    advanceTimeBy(1)
    runCurrent()
    assertThat(flow.value).isEqualTo(1)

    advanceTimeBy(100)
    runCurrent()
    assertThat(flow.value).isEqualTo(2)

    job.cancel()
  }

  @Test fun errorDelayedImmediate() = runTest {
    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    val exceptionHandler = RecordingExceptionHandler()

    var count by mutableStateOf(0)
    supervisorScope {
      var flow: StateFlow<Int>? = null

      val job = launch(start = CoroutineStart.UNDISPATCHED, context = exceptionHandler) {
        flow = launchMolecule(Immediate) {
          if (count == 1) {
            throw runtimeException
          }
          count
        }
      }

      assertThat(flow!!.value).isEqualTo(0)

      count++

      job.join()

      assertThat(exceptionHandler.exceptions.single()).isSameAs(runtimeException)
    }
  }
}
