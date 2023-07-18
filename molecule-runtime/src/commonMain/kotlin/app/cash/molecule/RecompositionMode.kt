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

import androidx.compose.runtime.MonotonicFrameClock
import kotlin.coroutines.CoroutineContext

/** The different recomposition modes of Molecule. */
public enum class RecompositionMode {
  /**
   * When a recomposition is needed, use a [MonotonicFrameClock] pulled from the calling [CoroutineContext]
   * to determine when to run. If no clock is found in the context, an exception is thrown.
   *
   * Use this option to drive Molecule with a built-in frame clock or a custom one.
   */
  ContextClock,

  /**
   * Run recomposition eagerly whenever one is needed.
   * Molecule will emit a new item every time the snapshot state is invalidated.
   */
  Immediate,
}
