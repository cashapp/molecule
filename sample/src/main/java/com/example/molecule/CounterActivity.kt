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

import android.app.Activity
import android.os.Bundle
import android.util.Log
import app.cash.molecule.AndroidUiDispatcher.Companion.Main
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.example.molecule.databinding.CounterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CounterActivity : Activity() {
  private val scope = CoroutineScope(Main)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val binding = CounterBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val events = binding.events()
      .onEach { event ->
        Log.d("CounterEvent", event.toString())
      }

    val randomService = RandomService()
    val models = scope.launchMolecule(clock = RecompositionClock.ContextClock) {
      CounterPresenter(events, randomService)
    }

    scope.launch(start = UNDISPATCHED) {
      models.collect { model ->
        Log.d("CounterModel", model.toString())
        binding.bind(model)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }
}
