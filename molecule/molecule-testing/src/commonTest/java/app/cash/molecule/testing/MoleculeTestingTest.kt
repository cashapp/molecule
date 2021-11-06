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

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
class MoleculeTestingTest {
  @Test
  fun timeoutCanBeZero() {
    testMolecule({ 1 }, timeoutMs = 0) {
      assertEquals(1, awaitItem())
    }
  }

  @Ignore("Does not work due to internal single-thread dispatcher usage")
  @Test
  fun timeoutEnforcedLong() {
    val dispatcher = TestCoroutineDispatcher()
    runBlocking(dispatcher) {
      testMolecule({ 1 }, timeoutMs = 10_000) {
        assertEquals(1, awaitItem())

        val nextItem = async { awaitItem() }
        dispatcher.advanceTimeBy(9_999)
        assertTrue(nextItem.isActive)

        dispatcher.advanceTimeBy(1)
        assertFalse(nextItem.isActive)

        val actual = assertFailsWith<TimeoutCancellationException> {
          nextItem.await()
        }
        assertEquals("Timed out waiting for 10000 ms", actual.message)
      }
    }
  }

  @Ignore("Does not work due to internal single-thread dispatcher usage")
  @Test
  @ExperimentalTime
  fun timeoutEnforcedDuration() {
    val dispatcher = TestCoroutineDispatcher()
    runBlocking(dispatcher) {
      testMolecule({ 1 }, timeout = Duration.seconds(10)) {
        assertEquals(1, awaitItem())

        val nextItem = async { awaitItem() }
        dispatcher.advanceTimeBy(9_999)
        assertTrue(nextItem.isActive)

        dispatcher.advanceTimeBy(1)
        assertFalse(nextItem.isActive)

        val actual = assertFailsWith<TimeoutCancellationException> {
          nextItem.await()
        }
        assertEquals("Timed out waiting for 10000 ms", actual.message)
      }
    }
  }

  @Test
  fun initialItemAvailableSynchronously() = runBlocking {
    var value = 0
    launch(start = UNDISPATCHED) {
      testMolecule({ 1 }) {
        value = awaitItem()
      }
    }
    assertEquals(1, value)
  }

  @Test
  fun flowOfWorks() {
    val flow = flowOf(1)
    testMolecule({
      val data by flow.collectAsState(null)
      data
    }) {
      assertNull(awaitItem())
      assertEquals(1, awaitItem())
    }
  }

  @Test
  fun sharedFlowWorks() {
    val flow = MutableSharedFlow<Int>()
    testMolecule({
      val data by flow.collectAsState(null)
      data
    }) {
      assertNull(awaitItem())
      flow.emit(1)
      assertEquals(1, awaitItem())
      flow.emit(2)
      assertEquals(2, awaitItem())
    }
  }

  @Test
  fun observableJustWorks() {
    val subject = Observable.just(1)
    testMolecule({
      val data by subject.subscribeAsState(null)
      data
    }) {
      assertNull(awaitItem())
      assertEquals(1, awaitItem())
    }
  }

  @Test
  fun behaviorSubjectWorks() {
    val subject = BehaviorSubject.createDefault(1)
    testMolecule({
      val data by subject.subscribeAsState(null)
      data
    }) {
      assertNull(awaitItem())
      assertEquals(1, awaitItem())
      subject.onNext(2)
      assertEquals(2, awaitItem())
    }
  }

  @Test
  fun behaviorSubjectAsFlowWorks() {
    val subject = BehaviorSubject.createDefault(1)
    val subjectFlow = subject.asFlow()
    testMolecule({
      val data by subjectFlow.collectAsState(null)
      data
    }) {
      assertNull(awaitItem())
      assertEquals(1, awaitItem())
      subject.onNext(2)
      assertEquals(2, awaitItem())
    }
  }

  @Test
  fun publishSubjectWorks() {
    val subject = PublishSubject.create<Int>()
    testMolecule({
      val data by subject.subscribeAsState(null)
      data
    }) {
      assertNull(awaitItem())
      subject.onNext(1)
      assertEquals(1, awaitItem())
    }
  }

  @Test
  fun publishSubjectAsFlowWorks() {
    val subject = PublishSubject.create<Int>()
    val subjectFlow = subject.asFlow()
    testMolecule({
      val data by subjectFlow.collectAsState(null)
      data
    }) {
      assertNull(awaitItem())
      subject.onNext(1)
      assertEquals(1, awaitItem())
    }
  }

  @Test
  fun immediateError() {
    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}

    testMolecule({
      throw runtimeException
    }) {
      assertSame(runtimeException, awaitError())
    }
  }

  @Test
  fun errorOnRecomposition() {
    // Use a custom subtype to prevent coroutines from breaking referential equality.
    val runtimeException = object : RuntimeException() {}

    val flow = MutableSharedFlow<Int>()
    testMolecule({
      val data by flow.collectAsState(null)

      if (data != null) {
        throw runtimeException
      } else {
        data
      }
    }) {
      assertNull(awaitItem())
      flow.emit(1)
      assertSame(runtimeException, awaitError())
    }
  }

  @Test
  fun errorWhenExpectingItem() {
    testMolecule<Unit>({
      throw RuntimeException()
    }) {
      val t = assertFailsWith<AssertionError> { awaitItem() }
      assertEquals(t.message, "Expected item but found Error(RuntimeException)")
    }
  }

  @Test
  fun itemWhenExpectingError() {
    testMolecule({}) {
      val t = assertFailsWith<AssertionError> { awaitError() }
      assertEquals(t.message, "Expected error but found Item(kotlin.Unit)")
    }
  }
}
