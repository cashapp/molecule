/*
 * Copyright (C) 2023 Square, Inc.
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
package com.example.molecule.viewmodel

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.Turbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PupperPicsPresenterTest {
  @Test
  fun `on launch, breeds are loaded followed by an image url`() = runBlocking {
    val picsService = FakePicsService()
    moleculeFlow(clock = RecompositionClock.Immediate) {
      PupperPicsPresenter(emptyFlow(), picsService)
    }.distinctUntilChanged().test {
      assertEquals(
        Model(
          loading = true,
          breeds = emptyList(),
          dropdownText = "Select breed",
          currentUrl = null,
        ),
        awaitItem(),
      )

      picsService.breeds.add(listOf("akita", "boxer", "corgi"))
      assertEquals(
        Model(
          loading = false,
          breeds = listOf("akita", "boxer", "corgi"),
          dropdownText = "akita",
          currentUrl = null,
        ),
        awaitItem(),
      )

      // After breeds are loaded, the first item in the list should be used to fetch an image URL.
      assertThat(picsService.urlRequestArgs.awaitItem()).isEqualTo("akita")

      picsService.urls.add("akita.jpg")
      assertEquals(
        Model(
          loading = false,
          breeds = listOf("akita", "boxer", "corgi"),
          dropdownText = "akita",
          currentUrl = "akita.jpg",
        ),
        awaitItem(),
      )
    }
  }

  @Test
  fun `selecting breed updates dropdown text and fetches new image`() = runBlocking {
    val picsService = FakePicsService()
    val events = Channel<Event>()
    moleculeFlow(clock = RecompositionClock.Immediate) {
      PupperPicsPresenter(events.receiveAsFlow(), picsService)
    }.distinctUntilChanged().test {
      picsService.breeds.add(listOf("akita", "boxer", "corgi"))
      picsService.urls.add("akita.jpg")
      assertThat(picsService.urlRequestArgs.awaitItem()).isEqualTo("akita")
      skipItems(3) // Fetching list, fetching fetching url, resolved model.

      events.send(Event.SelectBreed("boxer"))
      // Selecting a breed should see two emissions. One where the dropdown text changes, and
      // another where the URL is set to null.
      assertEquals(
        Model(
          loading = false,
          breeds = listOf("akita", "boxer", "corgi"),
          dropdownText = "boxer",
          currentUrl = "akita.jpg",
        ),
        awaitItem(),
      )
      assertEquals(
        Model(
          loading = false,
          breeds = listOf("akita", "boxer", "corgi"),
          dropdownText = "boxer",
          currentUrl = null,
        ),
        awaitItem(),
      )

      // We should then see a request for a boxer URL, followed by the model updating.
      assertThat(picsService.urlRequestArgs.awaitItem()).isEqualTo("boxer")
      picsService.urls.add("boxer.jpg")
      assertEquals(
        Model(
          loading = false,
          breeds = listOf("akita", "boxer", "corgi"),
          dropdownText = "boxer",
          currentUrl = "boxer.jpg",
        ),
        awaitItem(),
      )
    }
  }

  @Test
  fun `fetching again requests a new image`() = runBlocking {
    val picsService = FakePicsService()
    val events = Channel<Event>()
    moleculeFlow(clock = RecompositionClock.Immediate) {
      PupperPicsPresenter(events.receiveAsFlow(), picsService)
    }.distinctUntilChanged().test {
      picsService.breeds.add(listOf("akita", "boxer", "corgi"))
      assertThat(picsService.urlRequestArgs.awaitItem()).isEqualTo("akita")
      picsService.urls.add("akita1.jpg")
      skipItems(3) // Fetching list, fetching fetching url, resolved model.

      events.send(Event.FetchAgain)
      assertEquals(
        Model(
          loading = false,
          breeds = listOf("akita", "boxer", "corgi"),
          dropdownText = "akita",
          currentUrl = null,
        ),
        awaitItem(),
      )

      assertThat(picsService.urlRequestArgs.awaitItem()).isEqualTo("akita")
      picsService.urls.add("akita2.jpg")
      assertEquals(
        Model(
          loading = false,
          breeds = listOf("akita", "boxer", "corgi"),
          dropdownText = "akita",
          currentUrl = "akita2.jpg",
        ),
        awaitItem(),
      )
    }
  }

  private class FakePicsService : PupperPicsService {
    val breeds = Turbine<List<String>>()
    val urls = Turbine<String>()
    val urlRequestArgs = Turbine<String>()

    override suspend fun listBreeds(): List<String> {
      return breeds.awaitItem()
    }

    override suspend fun randomImageUrlFor(breed: String): String {
      urlRequestArgs.add(breed)
      return urls.awaitItem()
    }
  }
}
