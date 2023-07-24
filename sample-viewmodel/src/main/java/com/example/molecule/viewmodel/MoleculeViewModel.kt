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

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.moleculeFlow
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

abstract class MoleculeViewModel<Event, Model> : ViewModel() {
  private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)

  // Events have a capacity large enough to handle simultaneous UI events, but
  // small enough to surface issues if they get backed up for some reason.
  private val events = MutableSharedFlow<Event>(extraBufferCapacity = 20)

  val models: StateFlow<Model> by lazy(LazyThreadSafetyMode.NONE) {
    moleculeFlow(mode = ContextClock) {
      val presenter = remember { presenterFactory() }
      presenter.present(events)
    }.onEach {
      seed = it
    }.stateIn(
      scope = scope,
      started = SharingStarted.WhileSubscribed(5.seconds),
      initialValue = seed,
    )
  }

  fun take(event: Event) {
    if (!events.tryEmit(event)) {
      error("Event buffer overflow.")
    }
  }

  /**
   * This value serves as the initial value that the uiState [StateFlow] will emit and then as a
   * way to cache the last emission.
   * When the flow goes from being cold (when in the backstack and it has no observers) to being
   * hot again, by default the value cached using [stateIn] will be overwritten by the Presenter's
   * first emission. By default the presenter at that point won't have any notion of what that
   * cached value was without us providing this seed [Model].
   * It's the responsibility of the consumer to actually use this seed value when creating the
   * Presenter inside the [presenterFactory].
   */
  abstract var seed: Model

  /**
   * This will be remembered in the context of the moleculeFlow, so that it stays alive for as long
   * as the [models] [StateFlow] is still hot (has observers or the timeout hasn't timed out yet).
   */
  protected abstract fun presenterFactory(): MoleculePresenter<Event, Model>
}
