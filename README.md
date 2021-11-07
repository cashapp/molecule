# Molecule

Build a `StateFlow` stream using Jetpack Compose[^1].

```kotlin
fun CoroutineScope.launchCounter(): StateFlow<Int> = launchMolecule {
  val count by remember { mutableStateOf(0) }

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

In Cash App our presenter objects traditionally expose a single stream of display models through RxJava `Observable` or Kotlin coroutine's `Flow`.

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
) : ObservableTransformer<Nothing, ProfileModel> {
  override fun whevs(events: Observable<Nothing>): Observable<ProfileModel> {
    return combineLatest(
      db.userObservable().startWith(null),
      db.balanceObservable().startWith(0L),
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

This code is okay, but the RxJava ceremony will scale non-linearly.
This means the more sources of data which are used and the more complex the logic the harder to understand the Rx code becomes.

Despite emitting the `Loading` state synchronously, Compose UI [requires an initial value](https://developer.android.com/reference/kotlin/androidx/compose/runtime/rxjava3/package-summary#(io.reactivex.rxjava3.core.Observable).subscribeAsState(kotlin.Any)) be specified for all `Observable` usage.
This is a layering violation as the view layer is not in the position to dictate a reasonable default since the presenter layer controls the model object.

Molecule lets us fix both of these problems.
Our presenter can return a `StateFlow<ProfileModel>` whose initial state can be read synchronously at the view layer by Compose UI.
And by using Compose we also can build our model objects using imperative code built on features of the Kotlin language rather than reactive code consisting of RxJava library APIs.

```kotlin
@Composable
fun ProfilePresenter(
  userFlow: Observable<User>,
  balanceFlow: Observable<Long>,
): ProfileModel {
  val user by userFlow.subscribeAsState(null)
  val balance by balanceFlow.subscribeAsState(0L)

  return if (user == null) {
    Loading
  } else {
    Data(user.name, balance)
  }
}
```

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

## Usage

Add the buildscript dependency and apply the plugin to every module which wants to call `launchMolecule` or define `@Composable` functions for use with Molecule.

```groovy
buildscript {
  repository {
    mavenCental()
  }
  dependencies {
    classpath 'app.cash.molecule:molecule-gradle-plugin:0.1.0'
  }
}

apply plugin: 'app.cash.molecule'
```

<details>
<summary>Snapshots of the development version are available in Sonatype's snapshots repository.</summary>
<p>

```groovy
buildscript {
  repository {
    mavenCental()
    maven {
      url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
  }
  dependencies {
    classpath 'app.cash.molecule:molecule-gradle-plugin:0.2.0-SNAPSHOT'
  }
}

apply plugin: 'app.cash.molecule'
```

</p>
</details>

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
