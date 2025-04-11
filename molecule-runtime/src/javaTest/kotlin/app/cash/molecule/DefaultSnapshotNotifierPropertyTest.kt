/*
 * Copyright (C) 2025 Square, Inc.
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

import app.cash.molecule.SnapshotNotifier.External
import app.cash.molecule.SnapshotNotifier.WhileActive
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

// Note: We do not share this constant with the production code to verify its value doesn't change.
private const val property = "app.cash.molecule.snapshotNotifier"

class DefaultSnapshotNotifierPropertyTest {
  @Test fun propertyEmpty() {
    System.setProperty(property, "")
    try {
      assertThat(defaultSnapshotNotifier()).isEqualTo(WhileActive)
    } finally {
      System.clearProperty(property)
    }
  }

  @Test fun propertyInvalid() {
    System.setProperty(property, "sup")
    try {
      assertThat(defaultSnapshotNotifier()).isEqualTo(WhileActive)
    } finally {
      System.clearProperty(property)
    }
  }

  @Test fun propertyValid() {
    System.setProperty(property, "External")
    try {
      assertThat(defaultSnapshotNotifier()).isEqualTo(External)
    } finally {
      System.clearProperty(property)
    }
  }
}
