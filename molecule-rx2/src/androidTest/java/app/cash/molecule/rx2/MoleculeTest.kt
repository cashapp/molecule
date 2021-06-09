/*
 * Copyright (C) 2021 Square, Inc.
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
package app.cash.molecule.rx2

import app.cash.molecule.AndroidUiDispatcher.Companion.Main
import org.junit.Test

class MoleculeTest {
  @Test fun observableLitmus() {
    moleculeObservable(Main) { 0 }.test().assertValue(0).dispose()
  }

  @Test fun flowableLitmus() {
    moleculeFlowable(Main) { 0 }.test().assertValue(0).dispose()
  }
}
