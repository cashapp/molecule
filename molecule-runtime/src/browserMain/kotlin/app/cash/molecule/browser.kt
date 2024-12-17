/*
 * Copyright (C) 2024 Square, Inc.
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

// This file contains a subset of browser APIs normally provided by the Kotlin stdlib.
// However, these were recently removed in favor of kotlinx.browser which is not stable.
// Thus, we duplicate them for both JS and Wasm JS since this is a shared source set.

internal external val window: Window

internal external interface Window {
  val performance: Performance
  fun requestAnimationFrame(callback: (Double) -> Unit)
}

internal external interface Performance {
  fun now(): Double
}
