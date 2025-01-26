/*
 * Copyright (C) 2024 Square, Inc.
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

internal class MoleculeActiveContextElement(
  val isMoleculeActive: StateFlow<Boolean>,
) : AbstractCoroutineContextElement(Key) {
  companion object Key : CoroutineContext.Key<MoleculeActiveContextElement>
}

private val CoroutineScope.moleculeActiveFlow
  get() = coroutineContext.moleculeActiveFlow

private val CoroutineContext.moleculeActiveFlow
  get() = this[MoleculeActiveContextElement]?.isMoleculeActive
    ?: error("No Molecule context element found")

/**
 * Collects the specified flow as state, but only while the Molecule is active.
 */
@Composable
public fun <T> StateFlow<T>.collectAsStateWhileMoleculeActive(): State<T> =
  collectAsStateWhileMoleculeActive(this.value)

/**
 * Collects the specified flow as state, but only while the Molecule is active.
 *
 * @param initialValue the initial value to use as state.
 */
@Composable
public fun <T> Flow<T>.collectAsStateWhileMoleculeActive(initialValue: T): State<T> {
  val originalFlow = this
  val state = remember { mutableStateOf(initialValue) }
  LaunchedEffect(Unit) {
    repeatWhileMoleculeActive { originalFlow.collect { state.value = it } }
  }
  return state
}

/**
 * Runs the specified suspend block every time the molecule is active, cancelling it when the
 * molecule becomes inactive.
 */
public suspend fun CoroutineScope.repeatWhileMoleculeActive(block: suspend () -> Unit) {
  moleculeActiveFlow.collectLatest { isActive ->
    if (!isActive) return@collectLatest
    block()
  }
}

/**
 * Suspends until the Molecule is active.
 */
public suspend fun CoroutineScope.awaitMoleculeActive() {
  moleculeActiveFlow.first { it }
}
