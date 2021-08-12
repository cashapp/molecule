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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.AndroidUiDispatcher.Companion.Main
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MoleculeTest {
  @Test fun items() = runBlocking {
    val scope = CoroutineScope(Main)

    val flow = scope.launchMolecule {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(100) // TODO control time and frames in the test!
          count++
        }
      }

      count
    }

    assertEquals(0, flow.value)

    flow.test {
      assertEquals(0, awaitItem())
      assertEquals(1, awaitItem())
      assertEquals(2, awaitItem())
      cancel()
    }

    scope.cancel()
  }

  @Test fun errorImmediately() = runBlocking {
    val scope = CoroutineScope(Main)

    val runtimeException = RuntimeException()
    try {
      scope.launchMolecule {
        throw runtimeException
      }
      fail()
    } catch (t: Throwable) {
      assertEquals(runtimeException, t)
    }

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

  @Test fun errorDelayed() = runBlocking {
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(Main + exceptionHandler)

    val runtimeException = RuntimeException()
    var count by mutableStateOf(0)
    val flow = scope.launchMolecule {
      if (count == 1) {
        throw runtimeException
      }
      count
    }

    assertEquals(0, flow.value)

    flow.test {
      assertEquals(0, awaitItem())

      count++
      delay(100) // TODO control frames!
      assertEquals(listOf(runtimeException), exceptionHandler.exceptions)

      expectNoEvents()
      cancel()
    }

    scope.cancel()
  }

  @Test fun errorInEffect() = runBlocking {
    val exceptionHandler = RecordingExceptionHandler()
    val scope = CoroutineScope(Main + exceptionHandler)

    val runtimeException = RuntimeException()
    var count by mutableStateOf(0)
    val flow = scope.launchMolecule {
      LaunchedEffect(Unit) {
        delay(50)
        throw runtimeException
      }
      0
    }

    assertEquals(0, flow.value)

    flow.test {
      assertEquals(0, awaitItem())

      delay(100) // TODO control frames!
      assertEquals(listOf(runtimeException), exceptionHandler.exceptions)

      expectNoEvents()
      cancel()
    }

    scope.cancel()
  }
}
