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
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class MoleculeTest {
  @Test fun items() = runBlocking(Main) {
    val items = Channel<Int>()
    val job = launch {
      runMolecule(items::send) {
        var count by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
          while (true) {
            delay(100)
            count++
          }
        }

        Emit(count)
      }
    }

    assertEquals(0, items.receive())
    assertEquals(1, items.receive())
    assertEquals(2, items.receive())
    job.cancel()
  }

  @Test fun itemsCanBeSkipped() = runBlocking(Main) {
    val items = Channel<Int>()
    val job = launch {
      runMolecule(items::send) {
        var count by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
          while (true) {
            delay(100)
            count++
          }
        }

        if (count % 2 == 0) {
          Emit(count)
        } else {
          Skip
        }
      }
    }

    assertEquals(0, items.receive())
    assertEquals(2, items.receive())
    assertEquals(4, items.receive())
    job.cancel()
  }

  @Test fun firstItemSentImmediately() = runBlocking(Main) {
    val items = Channel<Int>()
    val job = launch(start = UNDISPATCHED) {
      runMolecule(items::send) {
        var count by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
          while (true) {
            delay(100)
            count++
          }
        }

        Emit(count)
      }
    }

    assertEquals(0, items.tryReceive().getOrNull())
    job.cancel()
  }

  @Test fun error() = runBlocking(Main) {
    val runtimeException = RuntimeException()
    try {
      runMolecule<Nothing>({ fail() }) {
        throw runtimeException
      }
      fail()
    } catch (t: Throwable) {
      assertSame(runtimeException, t)
    }
  }

  @Test fun errorInEffect() = runBlocking(Main) {
    val runtimeException = RuntimeException()
    try {
      runMolecule({ }) {
        val count by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
          delay(100)
          throw runtimeException
        }
        Emit(count)
      }
    } catch (t: Throwable) {
      assertSame(runtimeException, t)
    }
  }

  @Ignore("https://issuetracker.google.com/issues/169425431")
  @Test fun complete() = runBlocking(Main) {
    val items = Channel<Int>()
    runMolecule(items::send) { Emit(0) }

    assertEquals(0, items.receive())
  }

  @Ignore("https://issuetracker.google.com/issues/169425431")
  @Test fun completeWithEffect() = runBlocking(Main) {
    val items = Channel<Int>()
    runMolecule(items::send) {
      var count by remember { mutableStateOf(0) }
      LaunchedEffect(Unit) {
        repeat(2) {
          delay(100)
          count++
        }
      }

      Emit(count)
    }

    assertEquals(0, items.receive())
    assertEquals(1, items.receive())
    assertEquals(2, items.receive())
  }
}
