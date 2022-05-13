/*
 * Copyright (C) 2022 Square, Inc.
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

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.withTimeout
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@ExperimentalCoroutinesApi
class BackpressureMoleculeTest {
  @Test
  fun items() = runBlocking {
    val dispatcher = TestCoroutineDispatcher()
    val values = Channel<Int>(UNLIMITED)

    val job = launch(dispatcher) {
      backpressureMoleculeFlow {
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
    assertEquals(0, value)

    dispatcher.advanceTimeBy(100)
    value = values.awaitValue()
    assertEquals(1, value)

    dispatcher.advanceTimeBy(100)
    value = values.awaitValue()
    assertEquals(2, value)

    job.cancel()
  }

  @Test
  fun errors() = runBlocking {
    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    val t = assertFailsWith<RuntimeException> {
      backpressureMoleculeFlow {
        throw runtimeException
      }.collect()
    }

    assertSame(runtimeException, t)
  }

  @Test
  fun errorDelayed() = runBlocking {
    val values = Channel<Int>(UNLIMITED)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    var count by mutableStateOf(0)
    launch {
      val exception = kotlin.runCatching {
        backpressureMoleculeFlow {
          if (count == 1) {
            throw runtimeException
          }
          count
        }.collect {
          values.send(it)
        }
      }.exceptionOrNull()
      assertSame(exception, runtimeException)
    }

    assertEquals(0, values.awaitValue())

    count++
    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
  }

  @Test
  fun errorInEffect() = runBlocking {
    val values = Channel<Int>(UNLIMITED)

    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}
    launch {
      val exception = kotlin.runCatching {
        backpressureMoleculeFlow {
          LaunchedEffect(Unit) {
            delay(50)
            throw runtimeException
          }
          0
        }.collect {
          values.send(it)
        }
      }.exceptionOrNull()
      assertSame(exception, runtimeException)
    }

    assertEquals(0, values.awaitValue())

    Snapshot.sendApplyNotifications() // Ensure external state mutation is observed.
  }

  enum class DisposableEffectState { NOT_LAUNCHED, LAUNCHED, DISPOSED }

  @Test
  fun disposableEffectDisposesWhenScopeIsCancelled() = runBlocking {
    val values = Channel<Int>(UNLIMITED)

    var state: DisposableEffectState = DisposableEffectState.NOT_LAUNCHED

    val job = launch {
      backpressureMoleculeFlow {
        DisposableEffect(Unit) {
          state = DisposableEffectState.LAUNCHED

          onDispose {
            state = DisposableEffectState.DISPOSED
          }
        }
        0
      }.collect {
        values.send(it)
      }
    }

    assertEquals(0, values.awaitValue())

    job.cancelAndJoin()

    assertEquals(DisposableEffectState.DISPOSED, state)
  }
  private suspend fun <T> Channel<T>.awaitValue(): T = withTimeout(1000) { receive() }
}
