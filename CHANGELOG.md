# Change Log

## [Unreleased]


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



[Unreleased]: https://github.com/cashapp/molecule/compare/0.3.1...HEAD
[0.3.1]: https://github.com/cashapp/molecule/releases/tag/0.3.1
[0.3.0]: https://github.com/cashapp/molecule/releases/tag/0.3.0
[0.2.0]: https://github.com/cashapp/molecule/releases/tag/0.2.0
[0.1.0]: https://github.com/cashapp/molecule/releases/tag/0.1.0
