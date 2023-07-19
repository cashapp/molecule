# Molecule

Build a `StateFlow` or `Flow` stream using Jetpack Compose[^1].

```kotlin
fun CoroutineScope.launchCounter(): StateFlow<Int> = launchMolecule(mode = ContextClock) {
  var count by remember { mutableStateOf(0) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(1_000)
      count++
    }
  }

  count
}
```

[^1]: …and NOT Jetpack Compose UI!


## Introduction

Jetpack Compose UI makes it easy to build declarative UI with logic.

```kotlin
val userFlow = db.userObservable()
val balanceFlow = db.balanceObservable()

@Composable
fun Profile() {
  val user by userFlow.subscribeAsState(null)
  val balance by balanceFlow.subscribeAsState(0L)

  if (user == null) {
    Text("Loading…")
  } else {
    Text("${user.name} - $balance")
  }
}
```

Unfortunately, we are mixing business logic with display logic which makes testing harder than if it were separated.
The display layer is also interacting directly with the storage layer which creates undesirable coupling.
Additionally, if we want to power a different display with the same logic (potentially on another platform) we cannot.

Extracting the business logic to a presenter-like object fixes these three things.

In Cash App our presenter objects traditionally expose a single stream of display models through Kotlin coroutine's `Flow` or RxJava `Observable`.

```kotlin
sealed interface ProfileModel {
  object Loading : ProfileModel
  data class Data(
    val name: String,
    val balance: Long,
  ) : ProfileModel
}

class ProfilePresenter(
  private val db: Db,
) {
  fun transform(): Flow<ProfileModel> {
    return combine(
      db.users().onStart { emit(null) },
      db.balances().onStart { emit(0L) },
    ) { user, balance ->
      if (user == null) {
        Loading
      } else {
        Data(user.name, balance)
      }
    }
  }
}
```

This code is okay, but the ceremony of combining reactive streams will scale non-linearly.
This means the more sources of data which are used and the more complex the logic the harder to understand the reactive code becomes.

Despite emitting the `Loading` state synchronously, Compose UI [requires an initial value](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#(kotlinx.coroutines.flow.Flow).collectAsState(kotlin.Any,kotlin.coroutines.CoroutineContext)) be specified for all `Flow` or `Observable` usage.
This is a layering violation as the view layer is not in the position to dictate a reasonable default since the presenter layer controls the model object.

Molecule lets us fix both of these problems.
Our presenter can return a `StateFlow<ProfileModel>` whose initial state can be read synchronously at the view layer by Compose UI.
And by using Compose we also can build our model objects using imperative code built on features of the Kotlin language rather than reactive code consisting of RxJava library APIs.

```kotlin
@Composable
fun ProfilePresenter(
  userFlow: Flow<User>,
  balanceFlow: Flow<Long>,
): ProfileModel {
  val user by userFlow.collectAsState(null)
  val balance by balanceFlow.collectAsState(0L)

  return if (user == null) {
    Loading
  } else {
    Data(user.name, balance)
  }
}
```

This model-producing composable function can be run with `launchMolecule`.

```kotlin
val userFlow = db.users()
val balanceFlow = db.balances()
val models: StateFlow<ProfileModel> = scope.launchMolecule(mode = ContextClock) {
  ProfilePresenter(userFlow, balanceFlow)
}
```

A coroutine that runs `ProfilePresenter` and shares its output with the `StateFlow` is launched into the provided `CoroutineScope`.

At the view-layer, consuming the `StateFlow` of our model objects becomes trivial.

```kotlin
@Composable
fun Profile(models: StateFlow<ProfileModel>) {
  val model by models.collectAsState()
  when (model) {
    is Loading -> Text("Loading…")
    is Data -> Text("${model.name} - ${model.balance}")
  }
}
```

For more information see [the `launchMolecule` documentation](https://cashapp.github.io/molecule/docs/latest/molecule-runtime/app.cash.molecule/launch-molecule.html).

### Flow

In addition to `StateFlow`s, Molecule can create regular `Flow`s.

Here is the presenter example updated to use a regular `Flow`:
```kotlin
val userFlow = db.users()
val balanceFlow = db.balances()
val models: Flow<ProfileModel> = moleculeFlow(mode = Immediate) {
  ProfilePresenter(userFlow, balanceFlow)
}
```

And the counter example:
```kotlin
fun counter(): Flow<Int> = moleculeFlow(mode = Immediate) {
  var count by remember { mutableStateOf(0) }

  LaunchedEffect(Unit) {
    while (true) {
      delay(1_000)
      count++
    }
  }

  count
}
```

For more information see [the `moleculeFlow` documentation](https://cashapp.github.io/molecule/docs/latest/molecule-runtime/app.cash.molecule/molecule-flow.html).

## Usage

Add the buildscript dependency and apply the plugin to every module which wants to call `launchMolecule` or define `@Composable` functions for use with Molecule.

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'app.cash.molecule:molecule-gradle-plugin:1.0.0'
  }
}

apply plugin: 'app.cash.molecule'
```

Since Kotlin compiler plugins are an unstable API, certain versions of Molecule only work with
certain versions of Kotlin.

| Kotlin | Molecule       |
|--------|----------------|
| 1.8.22 | 0.11.0 - 1.0.0 |
| 1.8.21 | 0.10.0         |
| 1.8.20 | 0.9.0          |
| 1.8.10 | 0.8.0          |
| 1.8.0  | 0.7.0 - 0.7.1  |
| 1.7.20 | 0.6.0 - 0.6.1  |
| 1.7.10 | 0.4.0 - 0.5.0  |
| 1.7.0  | 0.3.0 - 0.3.1  |
| 1.6.10 | 0.2.0          |
| 1.5.31 | 0.1.0          |

<details>
<summary>Snapshots of the development version are available in Sonatype's snapshots repository.</summary>
<p>

```groovy
buildscript {
  repositories {
    mavenCentral()
    maven {
      url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
  }
  dependencies {
    classpath 'app.cash.molecule:molecule-gradle-plugin:1.1.0-SNAPSHOT'
  }
}

apply plugin: 'app.cash.molecule'
```

</p>
</details>

### Frame Clock

Whenever Jetpack Compose recomposes, it always waits for the next frame before beginning its work.
It is dependent on a `MonotonicFrameClock` in its `CoroutineContext` to know when a new frame is sent.
Molecule is just Jetpack Compose under the hood, so it also requires a frame clock: values won't be produced until a frame is sent and recomposition occurs.

Unlike Jetpack Compose, however, Molecule will sometimes be run in circumstances that do not provide a `MonotonicFrameClock`.
So all Molecule APIs require you to specify your preferred clock behavior:

* `RecompositionClock.ContextClock` behaves like Jetpack Compose: it will fish the `MonotonicFrameClock` out of the calling `coroutineContext` and use it for recomposition.
  If there is no `MonotonicFrameClock`, it will throw an exception.
  `ContextClock` is useful with Android's [`AndroidUiDispatcher.Main`](https://cashapp.github.io/molecule/docs/latest/molecule-runtime/app.cash.molecule/-android-ui-dispatcher/-companion/-main.html).
  `Main` has a built-in `MonotonicFrameClock` that is synchronized with the frame rate of the device.
  So a Molecule run on `Main` with `ContextClock` will run in lock step with the frame rate, too.
  Nifty!
  You can also provide your own `BroadcastFrameClock` to implement your own frame rate.
* `RecompositionClock.Immediate` will construct an immediate clock.
  This clock will produce a frame whenever the enclosing flow is ready to emit an item.
  (This is always the case for a `StateFlow`.)
  `Immediate` can be used where no clock is available at all without any additional wiring.
  It may be used for unit testing, or for running molecules off the main thread.

### Testing

Use `moleculeFlow(mode = Immediate)` and test using [Turbine](https://github.com/cashapp/turbine/). Your `moleculeFlow` will run just like any other flow does in Turbine.

```kotlin
@Test fun counter() = runTest {
  moleculeFlow(RecompositionClock.Immediate) {
    Counter()
  }.test {
    assertEquals(0, awaitItem())
    assertEquals(1, awaitItem())
    assertEquals(2, awaitItem())
    cancel()
  }
}
```


If you're unit testing Molecule on the JVM in an Android module, please set below in your project's AGP config.

```gradle
android {
  ...
  testOptions {
    unitTests.returnDefaultValues = true
  }
  ...
}
```

## License

    Copyright 2021 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
