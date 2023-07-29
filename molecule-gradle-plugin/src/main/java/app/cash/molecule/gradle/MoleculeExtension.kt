/*
 * Copyright (C) 2023 Square, Inc.
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
package app.cash.molecule.gradle

import org.gradle.api.provider.Property

interface MoleculeExtension {
  /**
   * The version of the JetBrains Compose compiler to use, or a Maven coordinate triple of
   * the custom Compose compiler to use.
   *
   * Example: using a custom version of the JetBrains Compose compiler
   * ```kotlin
   * redwood {
   *   kotlinCompilerPlugin.set("1.4.8")
   * }
   * ```
   *
   * Example: using a custom Maven coordinate for the Compose compiler
   * ```kotlin
   * redwood {
   *   kotlinCompilerPlugin.set("com.example:custom-compose-compiler:1.0.0")
   * }
   * ```
   */
  val kotlinCompilerPlugin: Property<String>
}
