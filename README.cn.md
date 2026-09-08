# Molecule

使用 Jetpack Compose 构建流式状态，类似于 `StateFlow` 或 `Flow`。

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

等等——我以为 Jetpack Compose 是 Android 的 UI 工具包？

确实，Compose 的核心是一个通用的编译器和运行时，用于状态跟踪和树节点/属性操作。它可以在 Kotlin 支持的任何平台上用于任何类型的状态或任何树结构。这是一项令人惊叹的技术。

Molecule "仅仅"将 Compose 的状态管理连接到 kotlinx.coroutines 的流，使其无需节点树即可使用。

![molecule is not a framework, just a headless compose ui meme](./molecule_not_a_framework_sign.jpg)

[^1]: ……而且不是 Jetpack Compose UI！


## 介绍

Jetpack Compose UI 使构建声明式 UI 变得简单。

```kotlin
val userFlow = db.userObservable()
val balanceFlow = db.balanceObservable()

@Composable
fun Profile() {
  val user by userFlow.subscribeAsState(null)
  val balance by balanceFlow.subscribeAsState(0L)

  if (user == null) {
    Text("加载中…")
  } else {
    Text("${user.name} - $balance")
  }
}
```

不幸的是，我们将业务逻辑与显示逻辑混合在了一起，这使得测试比分离时更难。显示层也与存储层直接交互，造成了不需要的耦合。此外，如果我们想在另一个平台上用相同的逻辑驱动不同的显示层，就无法实现。

将业务逻辑提取到类似 Presenter 的对象中可以解决这三个问题。

在 Cash App，我们的 Presenter 对象传统上通过 Kotlin 协程的 `Flow` 或 RxJava 的 `Observable` 暴露单一的显示模型流。

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

这段代码虽然可以，但组合响应式流的样板代码会非线性增长。这意味着使用的数据源越多、逻辑越复杂，响应式代码就越难理解。

尽管 `Loading` 状态是同步发送的，但 Compose UI 要求所有 `Flow` 或 `Observable` 的使用都必须指定初始值。这是一个分层违规问题，因为视图层无法控制合理的默认值，因为 Presenter 层控制着模型对象。

Molecule 让我们解决了这两个问题。我们的 Presenter 可以返回一个 `StateFlow<ProfileModel>`，其初始状态可以被 Compose UI 同步读取。而且，通过使用 Compose，我们还可以使用 Kotlin 语言特性构建模型对象，而不是使用由 RxJava 库 API 组成的响应式代码。

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

这个模型生成组合函数可以通过 `launchMolecule` 运行。

```kotlin
val userFlow = db.users()
val balanceFlow = db.balances()
val models: StateFlow<ProfileModel> = scope.launchMolecule(mode = ContextClock) {
  ProfilePresenter(userFlow, balanceFlow)
}
```

一个协程会在提供的 `CoroutineScope` 中运行 `ProfilePresenter` 并将其输出共享给 `StateFlow`。

在视图层，消费模型对象的 `StateFlow` 变得微不足道。

```kotlin
@Composable
fun Profile(models: StateFlow<ProfileModel>) {
  val model by models.collectAsState()
  when (model) {
    is Loading -> Text("加载中…")
    is Data -> Text("${model.name} - ${model.balance}")
  }
}
```

更多详细信息请参阅 [`launchMolecule`](https://cashapp.github.io/molecule/docs/latest/molecule-runtime/app.cash.molecule/launch-molecule.html) 文档。

### Flow

除了 `StateFlow` 之外，Molecule 还可以创建普通的 `Flow`。

这是使用普通 `Flow` 更新的 Presenter 示例：
```kotlin
val userFlow = db.users()
val balanceFlow = db.balances()
val models: Flow<ProfileModel> = moleculeFlow(mode = Immediate) {
  ProfilePresenter(userFlow, balanceFlow)
}
```

以及计数器示例：
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

更多详细信息请参阅 [`moleculeFlow`](https://cashapp.github.io/molecule/docs/latest/molecule-runtime/app.cash.molecule/molecule-flow.html) 文档。

## 使用方法

Molecule 是一个用于 Compose 的库，它依赖于 JetBrains 的 Kotlin Compose 插件才能使用。任何想要调用 `launchMolecule` 或定义用于 Molecule 的 `@Composable` 函数的模块都必须应用此插件。更多详细信息，请参阅 [JetBrains Compose 编译器文档](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compiler.html)。

然后可以将 Molecule 像其他依赖项一样添加：

```groovy
dependencies {
  implementation("app.cash.molecule:molecule-runtime:2.2.0")
}
```

<details>
<summary>开发版本快照可在 Central Portal Snapshots 仓库中获取</summary>
<p>

```groovy
repositories {
  mavenCentral()
  maven {
    url "https://central.sonatype.com/repository/maven-snapshots/"
  }
}

dependencies {
  implementation("app.cash.molecule:molecule-runtime:2.3.0-SNAPSHOT")
}
```

</p>
</details>

### 帧时钟

每当 Jetpack Compose 重新组合时，它总是等待下一帧才开始工作。它依赖于 `CoroutineContext` 中的 `MonotonicFrameClock` 来知道何时发送新帧。Molecule 底层就是 Jetpack Compose，所以它也需要帧时钟：直到发送帧并发生重新组合之前，才会生成值。

然而，与 Jetpack Compose 不同的是，Molecule 有时会在没有提供 `MonotonicFrameClock` 的情况下运行。因此，所有 Molecule API 都要求你指定首选的时钟行为：

* `RecompositionMode.ContextClock` 的行为类似于 Jetpack Compose：它会从调用的 `coroutineContext` 中提取 `MonotonicFrameClock` 并用于重新组合。
  如果没有 `MonotonicFrameClock`，它会抛出异常。
  `ContextClock` 与 Android 的 [`AndroidUiDispatcher.Main`](https://cashapp.github.io/molecule/docs/latest/molecule-runtime/app.cash.molecule/-android-ui-dispatcher/-companion/-main.html) 非常有用。
  `Main` 有一个与设备帧率同步的内置 `MonotonicFrameClock`。
  因此，在 `Main` 上使用 `ContextClock` 运行的 Molecule 也会与帧率同步运行。
  真是方便！
  你还可以提供自己的 `BroadcastFrameClock` 来实现自定义帧率。
* `RecompositionMode.Immediate` 将构造一个立即时钟。
  这个时钟会在包含的流准备好发出项目时产生一个帧。
  （对于 `StateFlow` 来说总是如此。）
  `Immediate` 可以在没有时钟的情况下使用，无需任何额外的接线。
  它可以用于单元测试，或在主线程之外运行 molecule。

### 测试

使用 `moleculeFlow(mode = Immediate)` 并使用 [Turbine](https://github.com/cashapp/turbine/) 进行测试。你的 `moleculeFlow` 将像 Turbine 中的任何其他流一样运行。

```kotlin
@Test fun counter() = runTest {
  moleculeFlow(RecompositionMode.Immediate) {
    Counter()
  }.test {
    assertEquals(0, awaitItem())
    assertEquals(1, awaitItem())
    assertEquals(2, awaitItem())
    cancel()
  }
}
```

如果你在 Android 模块的 JVM 上单元测试 Molecule，请在项目配置中设置以下内容。

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
