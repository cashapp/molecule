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
package app.cash.molecule

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Models the union type of `T | Skip` which indicates the action to be performed after each
 * recomposition of a molecule function.
 *
 * @see [moleculeFlow]
 */
sealed interface Action<out T>

/** Molecule action which emits an item to the underlying stream. */
class Emit<T>(val item: T) : Action<T>

/** Molecule action which skips emission to the underlying stream. */
object Skip : Action<Nothing>

fun <T> moleculeFlow(body: @Composable () -> Action<T>): Flow<T> {
  return flow {
    coroutineScope {
      val clock = checkNotNull(coroutineContext[MonotonicFrameClock]) {
        "Coroutine context must have a MonotonicFrameClock"
      }

      val recomposer = Recomposer(coroutineContext)
      val composition = Composition(UnitApplier, recomposer)
      launch(start = UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
      }

      var applyScheduled = false
      val snapshotHandle = Snapshot.registerGlobalWriteObserver {
        if (!applyScheduled) {
          applyScheduled = true
          launch {
            applyScheduled = false
            Snapshot.sendApplyNotifications()
          }
        }
      }

      try {
        var item by mutableStateOf<T?>(null)
        var valueChanged = false
        composition.setContent {
          val action = body()
          if (action is Emit) {
            item = action.item
            valueChanged = true
          }
        }

        while (true) {
          if (valueChanged) {
            valueChanged = false

            @Suppress("UNCHECKED_CAST") // Value guaranteed to be set from body().
            emit(item as T)
          }

          clock.withFrameNanos {}
        }
      } finally {
        snapshotHandle.dispose()
      }
    }
  }
}

private object UnitApplier : AbstractApplier<Unit>(Unit) {
  override fun insertBottomUp(index: Int, instance: Unit) {}
  override fun insertTopDown(index: Int, instance: Unit) {}
  override fun move(from: Int, to: Int, count: Int) {}
  override fun remove(index: Int, count: Int) {}
  override fun onClear() {}
}
