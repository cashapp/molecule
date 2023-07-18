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

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CounterPresenterTest {
  private val randomService = LocalRandomService(123)

  @Test
  fun localChanges() = runTest {
    val events = Channel<CounterEvent>()
    moleculeFlow(mode = RecompositionMode.Immediate) {
      CounterPresenter(events.receiveAsFlow(), randomService)
    }
      .test {
        assertThat(awaitItem()).isEqualTo(CounterModel(0, false))
        events.send(Change(+1))
        assertThat(awaitItem()).isEqualTo(CounterModel(1, false))
        events.send(Change(+1))
        assertThat(awaitItem()).isEqualTo(CounterModel(2, false))
        events.send(Change(-10))
        assertThat(awaitItem()).isEqualTo(CounterModel(-8, false))
      }
  }

  @Test
  fun randomChange() = runTest {
    val events = Channel<CounterEvent>()
    moleculeFlow(mode = RecompositionMode.Immediate) {
      CounterPresenter(events.receiveAsFlow(), randomService)
    }
      .test {
        assertThat(awaitItem()).isEqualTo(CounterModel(0, false))
        events.send(Randomize)
        assertThat(awaitItem()).isEqualTo(CounterModel(0, true))
        assertThat(awaitItem()).isEqualTo(CounterModel(18, false))
        events.send(Randomize)
        assertThat(awaitItem()).isEqualTo(CounterModel(18, true))
        assertThat(awaitItem()).isEqualTo(CounterModel(-4, false))
      }
  }
}
