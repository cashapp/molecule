/*
 * Copyright (C) 2022 Square, Inc.
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

enum class RecompositionClock {
  /**
   * Use the MonotonicFrameClock that already exists in the calling CoroutineContext.
   * If none exists, an exception is thrown.
   *
   * Use this option to drive Molecule with the built-in Android frame clock.
   */
  ContextClock,
  /**
   * Install an eagerly recomposing clock. This clock will provide a new frame immediately whenever
   * one is requested. The resulting flow will emit a new item every time the snapshot state is invalidated.
   */
  Immediate,
}
