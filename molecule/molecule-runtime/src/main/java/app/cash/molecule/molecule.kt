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
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

/**
 * Create a [Flow] which will continually recompose `body` to produce a stream of [T] values
 * when collected.
 *
 * The [CoroutineScope] in which the returned [Flow] is collected must contain a
 * [MonotonicFrameClock] key which controls when recomposition occurs.
 */
@OptIn(ExperimentalCoroutinesApi::class) // Marked as stable in kotlinx.coroutines 1.6.
fun <T> moleculeFlow(body: @Composable () -> T): Flow<T> {
  return channelFlow {
    launchMolecule(
      emitter = {
        trySend(it).getOrThrow()
      },
      body = body,
    )
  }
}

/**
 * Launch a coroutine into this [CoroutineScope] which will continually recompose `body`
 * to produce a [StateFlow] stream of [T] values.
 *
 * The [CoroutineScope] in which this [StateFlow] is created must contain a
 * [MonotonicFrameClock] key which controls when recomposition occurs.
 */
fun <T> CoroutineScope.launchMolecule(
  body: @Composable () -> T,
): StateFlow<T> {
  var flow: MutableStateFlow<T>? = null

  launchMolecule(
    emitter = { value ->
      val outputFlow = flow
      if (outputFlow != null) {
        outputFlow.value = value
      } else {
        flow = MutableStateFlow(value)
      }
    },
    body = body,
  )

  return flow!!
}

/**
 * Launch a coroutine into this [CoroutineScope] which will continually recompose `body`
 * to invoke [emitter] with each returned [T] value.
 *
 * The [CoroutineScope] in which this [StateFlow] is created must contain a
 * [MonotonicFrameClock] key which controls when recomposition occurs.
 */
fun <T> CoroutineScope.launchMolecule(
  emitter: (value: T) -> Unit,
  body: @Composable () -> T,
) {
  val recomposer = Recomposer(coroutineContext)
  val composition = Composition(UnitApplier, recomposer)
  composition.setContent {
    emitter(body())
  }
  val broadcastFrameClock = coroutineContext[MonotonicFrameClock] as? BroadcastFrameClock

  launch(start = UNDISPATCHED) {
    val globalWrites = Channel<Unit>(CONFLATED)
    val snapshotHandle = Snapshot.registerGlobalWriteObserver { globalWrites.trySend(Unit) }

    try {
      launch {
        globalWrites.consumeAsFlow().collect {
          Snapshot.sendApplyNotifications()

          // Recomposed values are always pulled for each composition frame, never pushed by state updates.
          // So if there is a broadcast frame clock in the launch scope (as is the case when producing
          // a StateFlow or Flow), a frame must be sent to force recomposition and pull a new value.
          broadcastFrameClock?.sendFrame(0)
        }
      }

      recomposer.runRecomposeAndApplyChanges()
    } finally {
      snapshotHandle.dispose()
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
