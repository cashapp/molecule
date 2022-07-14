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
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Create a [Flow] which will continually recompose `body` to produce a stream of [T] values
 * when collected.
 */
@OptIn(ExperimentalCoroutinesApi::class) // Marked as stable in kotlinx.coroutines 1.6.
fun <T> moleculeFlow(clock: RecompositionClock, body: @Composable () -> T): Flow<T> {
  return when (clock) {
    RecompositionClock.ContextClock -> contextClockFlow(body)
    RecompositionClock.Immediate -> immediateClockFlow(body)
  }
}

@OptIn(ExperimentalCoroutinesApi::class) // Marked as stable in kotlinx.coroutines 1.6.
private fun <T> contextClockFlow(body: @Composable () -> T) = channelFlow {
  launchMolecule(
    clock = RecompositionClock.ContextClock,
    emitter = {
      trySend(it).getOrThrow()
    },
    body = body,
  )
}

@OptIn(ExperimentalCoroutinesApi::class) // Marked as stable in kotlinx.coroutines 1.6.
private fun <T> immediateClockFlow(body: @Composable () -> T): Flow<T> = flow {
  coroutineScope {
    val clock = GatedFrameClock()
    val outputBuffer = Channel<T>(1)

    launch(clock, start = UNDISPATCHED) {
      launchMolecule(
        clock = RecompositionClock.ContextClock,
        emitter = {
          clock.isRunning = false
          outputBuffer.trySend(it).getOrThrow()
        },
        body = body,
      )
    }
    outputBuffer.consumeAsFlow().collect {
      emit(it)
      if (outputBuffer.isEmpty) clock.isRunning = true
    }
  }
}

/**
 * Launch a coroutine into this [CoroutineScope] which will continually recompose `body`
 * to produce a [StateFlow] stream of [T] values.
 */
fun <T> CoroutineScope.launchMolecule(
  clock: RecompositionClock,
  body: @Composable () -> T,
): StateFlow<T> {
  var flow: MutableStateFlow<T>? = null

  launchMolecule(
    clock = clock,
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
 * [launchMolecule]'s [emitter] is always free-running and will not respect backpressure.
 * Use [moleculeFlow] to create a backpressure-capable flow.
 */
fun <T> CoroutineScope.launchMolecule(
  clock: RecompositionClock,
  emitter: (value: T) -> Unit,
  body: @Composable () -> T,
) {
  val clockContext = when (clock) {
    RecompositionClock.ContextClock -> EmptyCoroutineContext
    RecompositionClock.Immediate -> GatedFrameClock().also { it.isRunning = true }
  }

  with(this + clockContext) {
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
    coroutineContext.job.invokeOnCompletion {
      composition.dispose()
      snapshotHandle.dispose()
    }

    composition.setContent {
      emitter(body())
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
