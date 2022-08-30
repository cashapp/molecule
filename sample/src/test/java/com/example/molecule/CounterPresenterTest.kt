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
package com.example.molecule

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterPresenterTest {
  private val randomService = LocalRandomService(123)

  @Test
  fun localChanges() = runTest {
    val events = Channel<CounterEvent>()
    moleculeFlow(clock = RecompositionClock.Immediate) {
      CounterPresenter(events.receiveAsFlow(), randomService)
    }
      .test {
        assertEquals(CounterModel(0, false), awaitItem())
        events.send(Change(+1))
        assertEquals(CounterModel(1, false), awaitItem())
        events.send(Change(+1))
        assertEquals(CounterModel(2, false), awaitItem())
        events.send(Change(-10))
        assertEquals(CounterModel(-8, false), awaitItem())
      }
  }

  @Test
  fun randomChange() = runTest {
    val events = Channel<CounterEvent>()
    moleculeFlow(clock = RecompositionClock.Immediate) {
      CounterPresenter(events.receiveAsFlow(), randomService)
    }
      .test {
        assertEquals(CounterModel(0, false), awaitItem())
        events.send(Randomize)
        assertEquals(CounterModel(0, true), awaitItem())
        assertEquals(CounterModel(18, false), awaitItem())
        events.send(Randomize)
        assertEquals(CounterModel(18, true), awaitItem())
        assertEquals(CounterModel(-4, false), awaitItem())
      }
  }
}
