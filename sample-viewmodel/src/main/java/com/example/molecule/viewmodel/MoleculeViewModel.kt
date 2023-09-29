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
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.moleculeFlow
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

abstract class MoleculeViewModel<Event, Model>(
  initialState: Model,
  started: SharingStarted = SharingStarted.WhileSubscribed(5.seconds),
  presenter: @Composable (seed: Model, events: Flow<Event>) -> Model,
) : ViewModel() {
  private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

  // Events have a capacity large enough to handle simultaneous UI events, but
  // small enough to surface issues if they get backed up for some reason.
  private val events = MutableSharedFlow<Event>(extraBufferCapacity = 20)

  private var seed: Model = initialState

  val models: StateFlow<Model> by lazy(LazyThreadSafetyMode.NONE) {
    moleculeFlow(mode = ContextClock) {
      presenter(seed, events)
    }.onEach {
      seed = it
    }.stateIn(
      scope = scope,
      started = started,
      initialValue = seed,
    )
  }

  fun take(event: Event) {
    if (!events.tryEmit(event)) {
      error("Event buffer overflow.")
    }
  }
}
