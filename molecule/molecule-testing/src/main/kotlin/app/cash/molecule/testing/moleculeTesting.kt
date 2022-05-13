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
package app.cash.molecule.testing

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameMillis
import app.cash.molecule.launchMolecule
import app.cash.molecule.RecompositionClock
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@Deprecated(
  "Use overload with Duration",
  ReplaceWith(
    "testMolecule(body, timeoutMs.milliseconds, validate)",
    "kotlin.time.Duration.Companion.milliseconds"
  ),
)
fun <T> testMolecule(
  body: @Composable () -> T,
  timeoutMs: Long,
  validate: suspend MoleculeTurbine<T>.() -> Unit,
) = testMolecule(body, timeoutMs.milliseconds, validate)

@ExperimentalCoroutinesApi
fun <T> testMolecule(
  body: @Composable () -> T,
  timeout: Duration = 1.seconds,
  validate: suspend MoleculeTurbine<T>.() -> Unit,
) = runBlocking {
  val events = Channel<Event<T>>(UNLIMITED)
  val exceptionHandler = EventEmittingExceptionHandler(events)
  val clock = BroadcastFrameClock()

  // Ensure exceptions in the molecule do not crash runBlocking. We redirect exceptions as events.
  val moleculeJob = SupervisorJob()

  launch(exceptionHandler + clock + moleculeJob, start = UNDISPATCHED) {
    try {
      launchMolecule(
        clock = RecompositionClock.ContextClock,
        emitter = { value ->
          val result = events.trySend(Event.Item(value))
          if (result.isFailure) {
            throw AssertionError("Unable to send item to events channel.")
          }
        },
        body = body,
      )
    } catch (t: Throwable) {
      val result = events.trySend(Event.Error(t))
      if (result.isFailure) {
        throw AssertionError("Unable to send error to events channel.")
      }
    }
  }

  try {
    val moleculeTurbine = TickOnDemandMoleculeTurbine(
      events = events,
      clock = clock,
      timeout = timeout,
    )

    moleculeTurbine.validate()
  } catch (t: Throwable) {
    while (true) {
      // If the validation lambda has thrown, search for an exception from the composable to
      // include which may help indicate the cause of the failure.
      val event = events.tryReceive().getOrNull() ?: break // No more items.
      if (event is Event.Error) {
        t.addSuppressed(event.throwable)
      }
    }
    throw t
  } finally {
    moleculeJob.cancelAndJoin()
  }
}

interface MoleculeTurbine<T> {
  /**
   * Assert that an event was received and return it.
   * If no events have been received, this function will suspend for up to [timeout].
   *
   * @throws kotlinx.coroutines.TimeoutCancellationException if no event was received in time.
   */
  public suspend fun awaitEvent(): Event<T>

  /**
   * Assert that the next event received was an item and return it.
   * If no events have been received, this function will suspend for up to [timeout].
   *
   * @throws AssertionError if the next event was completion or an error.
   * @throws kotlinx.coroutines.TimeoutCancellationException if no event was received in time.
   */
  public suspend fun awaitItem(): T

  /**
   * Assert that the next event received was an error terminating the flow.
   * If no events have been received, this function will suspend for up to [timeout].
   *
   * @throws AssertionError if the next event was an item or completion.
   * @throws kotlinx.coroutines.TimeoutCancellationException if no event was received in time.
   */
  public suspend fun awaitError(): Throwable
}

public sealed class Event<out T> {
  public data class Error(val throwable: Throwable) : Event<Nothing>() {
    override fun toString(): String = "Error(${throwable::class.simpleName})"
  }
  public data class Item<T>(val value: T) : Event<T>() {
    override fun toString(): String = "Item($value)"
  }
}

@ExperimentalCoroutinesApi
private class TickOnDemandMoleculeTurbine<T>(
  private val events: Channel<Event<T>>,
  private val clock: BroadcastFrameClock,
  private val timeout: Duration,
) : MoleculeTurbine<T> {
  private suspend fun <T> withTimeout(body: suspend () -> T): T {
    return if (timeout == Duration.ZERO) {
      body()
    } else {
      // TODO Migrate to just withTimeout(Duration) when we get coroutines 1.6.
      withTimeout(timeout.inWholeMilliseconds) {
        body()
      }
    }
  }

  override suspend fun awaitEvent(): Event<T> {
    return withTimeout {
      // Always yield once to give any launched coroutines a chance to execute before handing
      // control back to the caller.
      do {
        yieldAndAwaitFrame()
      } while (events.isEmpty)

      events.receive()
    }
  }

  override suspend fun awaitItem(): T {
    val event = awaitEvent()
    if (event !is Event.Item<T>) {
      unexpectedEvent(event, "item")
    }
    return event.value
  }

  override suspend fun awaitError(): Throwable {
    val event = awaitEvent()
    if (event !is Event.Error) {
      unexpectedEvent(event, "error")
    }
    return event.throwable
  }

  private fun unexpectedEvent(event: Event<*>, expected: String): Nothing {
    val cause = (event as? Event.Error)?.throwable
    throw AssertionError("Expected $expected but found $event", cause)
  }

  private suspend fun yieldAndAwaitFrame() {
    // Work could be scheduled on the current dispatcher, so yield before advancing the clock.
    yield()

    clock.awaitFrame()
  }

  private suspend fun BroadcastFrameClock.awaitFrame() {
    // TODO Remove the need for two frames to happen!
    //  I think this is because of the diff-sender is a hot loop that immediately reschedules
    //  itself on the clock. This schedules it ahead of the coroutine which applies changes and
    //  so we need to trigger an additional frame to actually emit the change's diffs.
    repeat(2) {
      coroutineScope {
        launch(start = UNDISPATCHED) {
          withFrameMillis { }
        }
        sendFrame(0L)
      }
    }
  }
}

private class EventEmittingExceptionHandler<T>(
  private val events: Channel<Event<T>>,
) : CoroutineExceptionHandler {
  override fun handleException(context: CoroutineContext, exception: Throwable) {
    val result = events.trySend(Event.Error(exception))
    if (result.isFailure) {
      throw AssertionError("Unable to send error to events channel.")
    }
  }

  override val key get() = CoroutineExceptionHandler
}
