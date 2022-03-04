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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class MoleculeTest {
  @Test fun items() {
    val dispatcher = TestCoroutineDispatcher()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(dispatcher + clock)
    var value: Int? = null

    scope.launchMolecule(emitter = { value = it }) {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(100)
          count++
        }
      }

      count
    }

    assertEquals(0, value)

    clock.sendFrame(0)
    assertEquals(0, value)

    dispatcher.advanceTimeBy(99)
    clock.sendFrame(0)
    assertEquals(0, value)

    dispatcher.advanceTimeBy(1)
    clock.sendFrame(0)
    assertEquals(1, value)

    dispatcher.advanceTimeBy(100)
    clock.sendFrame(0)
    assertEquals(2, value)

    scope.cancel()
  }

  @Test fun errorImmediately() {
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(clock)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    val t = assertFailsWith<RuntimeException> {
      scope.launchMolecule(emitter = { fail() }) {
        throw runtimeException
      }
    }
    assertSame(runtimeException, t)

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

  @Test fun errorDelayed() {
    val dispatcher = TestCoroutineDispatcher()
    val clock = BroadcastFrameClock()
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(dispatcher + clock + exceptionHandler)
    var value: Int? = null

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    var count by mutableStateOf(0)
    scope.launchMolecule(emitter = { value = it }) {
      if (count == 1) {
        throw runtimeException
      }
      count
    }

    assertEquals(0, value)

    count++
    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
    clock.sendFrame(0)
    assertSame(runtimeException, exceptionHandler.exceptions.single())

    scope.cancel()
  }

  @Test fun errorInEffect() {
    val dispatcher = TestCoroutineDispatcher()
    val clock = BroadcastFrameClock()
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(dispatcher + clock + exceptionHandler)
    var value: Int? = null

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    scope.launchMolecule(emitter = { value = it }) {
      LaunchedEffect(Unit) {
        delay(50)
        throw runtimeException
      }
      0
    }

    assertEquals(0, value)

    dispatcher.advanceTimeBy(50)
    clock.sendFrame(0)
    assertSame(runtimeException, exceptionHandler.exceptions.single())

    scope.cancel()
  }

  @Test fun errorInEmitterImmediately() {
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(clock)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    val t = assertFailsWith<RuntimeException> {
      scope.launchMolecule(emitter = { throw runtimeException }) {
        0
      }
    }

    assertSame(runtimeException, t)

    scope.cancel()
  }

  @Test fun errorInEmitterDelayed() {
    val dispatcher = TestCoroutineDispatcher()
    val clock = BroadcastFrameClock()
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(dispatcher + clock + exceptionHandler)
    var value: Int? = null

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    var count by mutableStateOf(0)
    scope.launchMolecule(
      emitter = {
        if (it == 1) {
          throw runtimeException
        }
        value = it
      }
    ) {
      count
    }

    assertEquals(0, value)

    count++
    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
    clock.sendFrame(0)
    assertSame(runtimeException, exceptionHandler.exceptions.single())

    scope.cancel()
  }

  enum class DisposableEffectState { NOT_LAUNCHED, LAUNCHED, DISPOSED }

  @Test fun disposableEffectDisposesWhenScopeIsCancelled() {
    val dispatcher = TestCoroutineDispatcher()
    val clock = BroadcastFrameClock()
    val scope = CoroutineScope(dispatcher + clock)

    var state: DisposableEffectState = DisposableEffectState.NOT_LAUNCHED

    scope.launchMolecule {
      DisposableEffect(Unit) {
        state = DisposableEffectState.LAUNCHED

        onDispose {
          state = DisposableEffectState.DISPOSED
        }
      }
    }

    assertEquals(DisposableEffectState.LAUNCHED, state)

    scope.cancel()
    assertEquals(DisposableEffectState.DISPOSED, state)
  }
}
