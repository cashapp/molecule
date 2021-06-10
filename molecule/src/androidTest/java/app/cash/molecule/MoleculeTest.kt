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
import app.cash.turbine.test
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertSame
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MoleculeTest {
  @Test fun items() = runBlocking(AndroidUiDispatcher.Main) {
    val flow = moleculeFlow {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(100)
          count++
        }
      }

      count
    }

    flow.test {
      assertEquals(0, expectItem())
      assertEquals(1, expectItem())
      assertEquals(2, expectItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun firstItemSentImmediately() = runBlocking(AndroidUiDispatcher.Main) {
    val flow = moleculeFlow {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        while (true) {
          delay(100)
          count++
        }
      }

      count
    }

    var value = -1
    val job = launch(start = UNDISPATCHED) {
      flow.collect {
        value = it
      }
    }
    assertEquals(0, value)
    job.cancel()
  }

  @Test fun error() = runBlocking(AndroidUiDispatcher.Main) {
    val runtimeException = RuntimeException()
    val flow = moleculeFlow {
      throw runtimeException
    }

    flow.test {
      assertSame(runtimeException, expectError())
    }
  }

  @Test fun errorInEffect() = runBlocking(AndroidUiDispatcher.Main) {
    val runtimeException = RuntimeException()
    val flow = moleculeFlow {
      val count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        delay(100)
        throw runtimeException
      }
      count
    }

    flow.test {
      assertEquals(0, expectItem())
      assertSame(runtimeException, expectError())
    }
  }

  @Ignore("https://issuetracker.google.com/issues/169425431")
  @Test fun complete() = runBlocking(AndroidUiDispatcher.Main) {
    val flow = moleculeFlow { 0 }

    flow.test {
      assertEquals(0, expectItem())
      expectComplete()
    }
  }

  @Ignore("https://issuetracker.google.com/issues/169425431")
  @Test fun completeWithEffect() = runBlocking(AndroidUiDispatcher.Main) {
    val flow = moleculeFlow {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        repeat(2) {
          delay(100)
          count++
        }
      }

      count
    }

    flow.test {
      assertEquals(0, expectItem())
      assertEquals(1, expectItem())
      assertEquals(2, expectItem())
      expectComplete()
    }
  }
}
