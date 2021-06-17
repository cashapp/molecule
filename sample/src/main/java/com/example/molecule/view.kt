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

import com.example.molecule.databinding.CounterBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun CounterBinding.events() = callbackFlow {
  decreaseTen.setOnClickListener { trySend(Change(-10)) }
  decreaseOne.setOnClickListener { trySend(Change(-1)) }
  increaseOne.setOnClickListener { trySend(Change(1)) }
  increaseTen.setOnClickListener { trySend(Change(10)) }
  randomize.setOnClickListener { trySend(Randomize) }

  awaitClose { }
}

fun CounterBinding.bind(model: CounterModel) {
  decreaseTen.isEnabled = !model.loading
  decreaseOne.isEnabled = !model.loading
  increaseOne.isEnabled = !model.loading
  increaseTen.isEnabled = !model.loading
  randomize.isEnabled = !model.loading

  count.text = model.value.toString()
}
