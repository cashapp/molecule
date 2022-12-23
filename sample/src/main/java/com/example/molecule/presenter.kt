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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface CounterEvent
data class Change(val delta: Int) : CounterEvent
object Randomize : CounterEvent

data class CounterModel(
  val value: Int,
  val loading: Boolean,
)

@Composable
fun CounterPresenter(
  events: Flow<CounterEvent>,
  randomService: RandomService,
): CounterModel {
  var count by remember { mutableStateOf(0) }
  var loading by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    events.collect { event ->
      when (event) {
        is Change -> {
          count += event.delta
        }
        Randomize -> {
          loading = true
          launch {
            // We want to observe these two state changes atomically.
            Snapshot.withMutableSnapshot {
              count = randomService.get(-20, 20)
              loading = false
            }
          }
        }
      }
    }
  }

  return CounterModel(count, loading)
}
