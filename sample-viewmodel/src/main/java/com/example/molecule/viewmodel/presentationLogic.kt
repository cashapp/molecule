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
package com.example.molecule.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow

sealed interface Event {
  data class SelectBreed(val breed: String) : Event
  object FetchAgain : Event
}

data class Model(
  val loading: Boolean,
  val breeds: List<String>,
  val dropdownText: String,
  val currentUrl: String?,
)

class PupperPicsViewModel : MoleculeViewModel<Event, Model>() {
  @Composable
  override fun models(events: Flow<Event>): Model {
    return pupperPicsPresenter(events, PupperPicsService())
  }
}

@Composable
fun pupperPicsPresenter(events: Flow<Event>, service: PupperPicsService): Model {
  var breeds: List<String> by remember { mutableStateOf(emptyList()) }
  var currentBreed: String? by remember { mutableStateOf(null) }
  var currentUrl: String? by remember { mutableStateOf(null) }
  var fetchId: Int by remember { mutableStateOf(0) }

  // Grab the list of breeds and sets the current selection to the first in the list.
  // Errors are ignored in this sample.
  LaunchedEffect(Unit) {
    breeds = service.listBreeds()
    currentBreed = breeds.first()
  }

  // Load a random URL for the current breed whenever it changes, or the fetchId changes.
  LaunchedEffect(currentBreed, fetchId) {
    currentUrl = null
    currentUrl = currentBreed?.let { service.randomImageUrlFor(it) }
  }

  // Handle UI events.
  LaunchedEffect(Unit) {
    events.collect { event ->
      when (event) {
        is Event.SelectBreed -> currentBreed = event.breed
        Event.FetchAgain -> fetchId++ // Incrementing fetchId will load another random image URL.
      }
    }
  }

  return Model(
    loading = currentBreed == null,
    breeds = breeds,
    dropdownText = currentBreed ?: "Select breed",
    currentUrl = currentUrl,
  )
}
