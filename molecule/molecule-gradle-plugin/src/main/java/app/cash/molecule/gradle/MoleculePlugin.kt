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
package app.cash.molecule.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class MoleculePlugin : KotlinCompilerPluginSupportPlugin {
  override fun apply(target: Project) {
    target.configurations.all { configuration ->
      configuration.resolutionStrategy.eachDependency {
        if (it.requested.group == "app.cash.molecule" &&
          it.requested.name == "molecule-testing" &&
          it.requested.version == ""
        ) {
          it.useVersion(moleculeVersion)
          it.because("Matches the version of the Molecule Gradle plugin and runtime")
        }
      }
    }
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun getCompilerPluginId(): String = "app.cash.molecule"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    "androidx.compose.compiler",
    "compiler",
    composeVersion,
  )

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    kotlinCompilation.dependencies {
      implementation("app.cash.molecule:molecule-runtime:$moleculeVersion")
    }

    return kotlinCompilation.target.project.provider { emptyList() }
  }
}
