# Change Log

## [Unreleased]


## [1.3.0] - 2023-10-31 🎃

New:
- Add `CoroutineContext` parameter to `launchMolecule` to contribute elements to the combined
  context that is used for running Compose.
- Support for Kotlin 1.9.20

This version works with Kotlin 1.9.20 by default.


## [1.2.1] - 2023-09-14

New:
- Support for Kotlin 1.9.10
- Switch to JetBrains Compose compiler 1.5.2 (based on AndroidX Compose compiler 1.5.3)

This version works with Kotlin 1.9.10 by default.


## [1.2.0] - 2023-08-09

New:
- Support for specifying custom Compose compiler versions. This will allow you to use the latest
  version of Molecule with newer versions of Kotlin than it explicitly supports.

  See [the README](https://github.com/cashapp/molecule/#custom-compose-compiler) for more information.

Fixed:
- Ensure frame times sent by `RecompositionMode.Immediate` always increase. Previously,
  when targeting JS, the same frame time could be seen since the clock only has millisecond
  precision. Since the frame time is in nanoseconds, synthetic nanosecond offsets will be added to
  ensure each timestamp is strictly greater than the last.
- Perform teardown of the composition on cancellation within an existing coroutine rather than in
  a job completion listener. This ensures it executes on the same dispatcher as the rest of the
  system, rather than on the canceling caller's thread.


## [1.1.0] - 2023-07-20

New:
- Support for Kotlin 1.9.0
- Switch to JetBrains Compose compiler 1.5.0 (based on AndroidX Compose compiler 1.5.0)


## [1.0.0] - 2023-07-19

Changed:
- `RecompositionClock` is now named `RecompositionMode` to better reflect that it is not itself the clock,
  but the mode by which Molecule will perform recomposition. A clock is always used internally as that is the
  underlying mechanism of Compose.
- Darwin frame clock and the internal frame clock used with `RecompositionMode.Immediate` now correctly
  send actual frame times.


## [0.11.0] - 2023-06-30

New:
- Support for Kotlin 1.8.22
- Switch to JetBrains Compose compiler 1.4.8 (AndroidX Compose compiler 1.4.8)


## [0.10.0] - 2023-06-26

New:
- Support for Kotlin 1.8.21
- Update to JetBrains Compose runtime 1.4.1 (AndroidX Compose runtime 1.4.3).
- Switch to JetBrains Compose compiler 1.4.7 (AndroidX Compose compiler 1.4.7)


## [0.9.0] - 2023-04-12

New:
- Support for Kotlin 1.8.20
- Update to JetBrains Compose runtime 1.4.0 (AndroidX Compose runtime 1.4.0).
- Switch to JetBrains Compose compiler 1.4.5 (as yet unreleased AndroidX Compose compiler, probably 1.4.5)


## [0.8.0] - 2023-03-09

New:
- Support for Kotlin 1.8.10
- Update to JetBrains Compose runtime 1.3.1 (AndroidX Compose runtime 1.2.1).
- Switch to JetBrains Compose compiler 1.3.2.1 (AndroidX Compose compiler 1.3.2 + JS fix)


## [0.7.1] - 2023-02-20

New:
- Add `WindowAnimationFrameClock` for use in browser-based JS environments.

Changed:
- Switch to JetBrains Compose compiler which has better support for JS and native targets.


## [0.7.0] - 2023-01-17

New:
- Support for Kotlin 1.8.0
- Switch (back) to AndroidX Compose compiler 1.4.0


## [0.6.1] - 2022-11-16

New:
- Add support for `watchosArm32` and `watchosX86` native targets.


## [0.6.0] - 2022-11-08

New:
- Support for Kotlin 1.7.20
- Update to JetBrains Compose runtime 1.2.1 (AndroidX Compose runtime 1.2.1).
- Switch to JetBrains Compose compiler 1.3.2.1 (AndroidX Compose compiler 1.3.2 + JS fix)

Fixed:
- When applying the Compose compiler plugin to Kotlin/JS targets, ensure decoys are used.
- Add `cacheKind=none` Gradle configuration which ensures downstream Kotlin/Native projects can link.


## [0.5.0] - 2022-10-13

New:

 - Update to JetBrains Compose runtime 1.2.0 (this uses AndroidX Compose runtime 1.2.1).
 - Add iOS, MacOS, tvOS, watchOS, linux, and windows targets for Kotlin/Native.

Changed:

 - The 'molecule-testing' artifact has been removed.


## [0.5.0-beta01] - 2022-09-16

New:

 - Update to JetBrains Compose runtime 1.2.0-beta01 (this uses AndroidX Compose runtime 1.2.1).
 - Add iOS, MacOS, tvOS, watchOS, linux, and windows targets for Kotlin/Native.

Changed:

 - The 'molecule-testing' artifact has been removed.


## [0.4.0] - 2022-08-10

New:

 - Update to Compose compiler 1.3.0 which supports Kotlin 1.7.10.

Fixed:

 - Prevent "Trying to call 'getOrThrow' on a failed channel result: Failed" exceptions when using the immediate recompose clock.


## [0.4.0-beta01] - 2022-07-27

New:

 - Update to Compose compiler 1.3.0-beta01 which supports Kotlin 1.7.10.


## [0.3.1] - 2022-08-10

Fixed:

 - Prevent "Trying to call 'getOrThrow' on a failed channel result: Failed" exceptions when using the immediate recompose clock.


## [0.3.0] - 2022-07-27

New:

 - Enable Kotlin multiplatform usage on JVM and JS targets (in addition to Android). All native targets are blocked on JetBrains' Compose runtime supporting them (with a stable release).
 - Update to Compose compiler 1.2.0 which supports Kotlin 1.7.0.
 - Add `RecomposeClock` parameter to both `moleculeFlow` and `launchMolecule` which allows choosing between a frame-based clock for recomposition or a clock which immediately recomposes for any change.
 - The 'molecule-testing' library is deprecated. The recommendation is to use the new immediate clock mode and [Turbine](https://github.com/cashapp/turbine/). If you have a use case which cannot be handled by this change please comment on [this issue](https://github.com/cashapp/molecule/issues/97).


## [0.2.0] - 2022-02-09

New:

 - Update to Compose 1.1.0 which supports Kotlin 1.6.10

Fixed:

 - Explicitly dispose internal `Composition` allowing `DisposableEffect`s to fire.


## [0.1.0] - 2021-11-10

Initial release



[Unreleased]: https://github.com/cashapp/molecule/compare/1.2.0...HEAD
[1.2.0]: https://github.com/cashapp/molecule/releases/tag/1.2.0
[1.1.0]: https://github.com/cashapp/molecule/releases/tag/1.1.0
[1.0.0]: https://github.com/cashapp/molecule/releases/tag/1.0.0
[0.11.0]: https://github.com/cashapp/molecule/releases/tag/0.11.0
[0.10.0]: https://github.com/cashapp/molecule/releases/tag/0.10.0
[0.9.0]: https://github.com/cashapp/molecule/releases/tag/0.9.0
[0.8.0]: https://github.com/cashapp/molecule/releases/tag/0.8.0
[0.7.1]: https://github.com/cashapp/molecule/releases/tag/0.7.1
[0.7.0]: https://github.com/cashapp/molecule/releases/tag/0.7.0
[0.6.1]: https://github.com/cashapp/molecule/releases/tag/0.6.1
[0.6.0]: https://github.com/cashapp/molecule/releases/tag/0.6.0
[0.5.0]: https://github.com/cashapp/molecule/releases/tag/0.5.0
[0.5.0-beta01]: https://github.com/cashapp/molecule/releases/tag/0.5.0-beta01
[0.4.0]: https://github.com/cashapp/molecule/releases/tag/0.4.0
[0.4.0-beta01]: https://github.com/cashapp/molecule/releases/tag/0.4.0-beta01
[0.3.1]: https://github.com/cashapp/molecule/releases/tag/0.3.1
[0.3.0]: https://github.com/cashapp/molecule/releases/tag/0.3.0
[0.2.0]: https://github.com/cashapp/molecule/releases/tag/0.2.0
[0.1.0]: https://github.com/cashapp/molecule/releases/tag/0.1.0
