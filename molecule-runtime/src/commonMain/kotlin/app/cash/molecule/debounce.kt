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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlinx.coroutines.delay

/**
 * Returns a debounced version of [value] that only updates after [value] has been
 * stable (unchanged) for the given [timeout] duration.
 *
 * The initial [value] is returned immediately on first composition with no delay.
 * On subsequent changes to [value], a timer of [timeout] is started. If [value]
 * changes again before the timer expires, the timer resets. The returned value only
 * updates once [value] has remained stable for the full [timeout] duration.
 */
@Composable
public fun <T> rememberDebounced(value: T, timeout: Duration): T {
  var debouncedValue by remember { mutableStateOf(value) }
  LaunchedEffect(value) {
    delay(timeout)
    debouncedValue = value
  }
  return debouncedValue
}
