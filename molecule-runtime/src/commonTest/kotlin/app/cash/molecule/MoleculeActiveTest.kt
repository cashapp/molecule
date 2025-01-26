package app.cash.molecule

import androidx.compose.runtime.LaunchedEffect
import app.cash.molecule.RecompositionMode.Immediate
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MoleculeActiveTest {

  @Test
  fun moleculeInactive_flowCold() = runTest {
    cancelWhenDone {
      val flow = flow { emit(1) }

      val stateFlow = launchMolecule(Immediate) {
        flow.collectAsStateWhileMoleculeActive(0).value
      }

      runCurrent()
      assertThat(stateFlow.value).isEqualTo(0)
    }
  }

  @Test
  fun moleculeActive_flowHot() = runTest {
    cancelWhenDone {
      val flow = flow { emit(1) }

      val stateFlow = launchMolecule(Immediate) {
        flow.collectAsStateWhileMoleculeActive(0).value
      }

      launch { stateFlow.collect { } }

      runCurrent()
      assertThat(stateFlow.value).isEqualTo(1)
    }
  }


  @Test
  fun moleculeActiveThenInactive_flowCold() = runTest {
    cancelWhenDone {
      val flow = flow {
        emit(1)
        delay(1)
        emit(2)
      }

      val stateFlow = launchMolecule(Immediate) {
        flow.collectAsStateWhileMoleculeActive(0).value
      }

      val collectJob = launch { stateFlow.collect { } }

      runCurrent()
      collectJob.cancel()

      advanceTimeBy(1)
      runCurrent()
      assertThat(stateFlow.value).isEqualTo(1)
    }
  }


  @Test
  fun moleculeInactive_stateFlowCold() = runTest {
    cancelWhenDone {
      val flow = flow { emit(1) }.stateIn(this, SharingStarted.WhileSubscribed(), 0)

      val stateFlow = launchMolecule(Immediate) {
        flow.collectAsStateWhileMoleculeActive().value
      }

      runCurrent()
      assertThat(stateFlow.value).isEqualTo(0)
    }
  }

  @Test
  fun moleculeActive_stateFlowHot() = runTest {
    cancelWhenDone {
      val flow = flow { emit(1) }.stateIn(this, SharingStarted.WhileSubscribed(), 0)

      val stateFlow = launchMolecule(Immediate) {
        flow.collectAsStateWhileMoleculeActive().value
      }

      launch { stateFlow.collect { } }

      runCurrent()
      assertThat(stateFlow.value).isEqualTo(1)
    }
  }


  @Test
  fun moleculeActiveThenInactive_stateFlowCold() = runTest {
    cancelWhenDone {
      val flow = flow {
        emit(1)
        delay(1)
        emit(2)
      }.stateIn(this, SharingStarted.WhileSubscribed(), 0)

      val stateFlow = launchMolecule(Immediate) {
        flow.collectAsStateWhileMoleculeActive().value
      }

      val collectJob = launch { stateFlow.collect { } }

      runCurrent()
      collectJob.cancel()

      advanceTimeBy(1)
      runCurrent()
      assertThat(stateFlow.value).isEqualTo(1)
    }
  }

  @Test
  fun moleculeInactive_coroutineDoesNotRun() = runTest {
    cancelWhenDone {
      var coroutineRan = false
      val stateFlow = launchMolecule(Immediate) {
        LaunchedEffect(Unit) {
          repeatWhileMoleculeActive {
            coroutineRan = true
          }
        }
      }

      runCurrent()

      assertThat(coroutineRan).isEqualTo(false)
    }
  }

  @Test
  fun moleculeActive_coroutineRuns() = runTest {
    cancelWhenDone {
      var coroutineRan = false
      val stateFlow = launchMolecule(Immediate) {
        LaunchedEffect(Unit) {
          repeatWhileMoleculeActive {
            coroutineRan = true
          }
        }
      }

      launch { stateFlow.collect {} }
      runCurrent()

      assertThat(coroutineRan).isEqualTo(true)
    }
  }

  @Test
  fun moleculeActiveInactiveThenActive_coroutineRestarted() = runTest {
    cancelWhenDone {
      var coroutineRunCount = 0
      val stateFlow = launchMolecule(Immediate) {
        LaunchedEffect(Unit) {
          repeatWhileMoleculeActive {
            while (true) {
              delay(1)
              coroutineRunCount++
            }
          }
        }
      }

      val firstCollector = launch { stateFlow.collect {} }
      advanceTimeBy(1)
      runCurrent()
      assertThat(coroutineRunCount).isEqualTo(1)

      firstCollector.cancel()
      advanceTimeBy(1)
      runCurrent()
      assertThat(coroutineRunCount).isEqualTo(1)

      launch { stateFlow.collect {} }
      advanceTimeBy(1)
      runCurrent()
      assertThat(coroutineRunCount).isEqualTo(2)
    }
  }

  @Test
  fun moleculeInactive_suspends() = runTest {
    cancelWhenDone {
      var ran = false
      val stateFlow = launchMolecule(Immediate) {
        LaunchedEffect(Unit) {
          awaitMoleculeActive()
          ran = true
        }
      }

      runCurrent()
      assertThat(ran).isFalse()
    }
  }

  @Test
  fun moleculeActive_doesNotSuspend() = runTest {
    cancelWhenDone {
      var ran = false
      val stateFlow = launchMolecule(Immediate) {
        LaunchedEffect(Unit) {
          awaitMoleculeActive()
          ran = true
        }
      }

      launch { stateFlow.collect {} }
      runCurrent()
      assertThat(ran).isTrue()
    }
  }

  private suspend fun CoroutineScope.cancelWhenDone(block: suspend CoroutineScope.() -> Unit) {
    val job = Job(coroutineContext.job)
    val scope = this + job
    block(scope)
    job.cancel()
  }
}
